package com.auditbucket.client;

import au.com.bytecode.opencsv.CSVReader;
import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.bean.TagInputBean;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jdom2.JDOMException;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
@SuppressWarnings("StatementWithEmptyBody")
public class Importer {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Importer.class);

    public enum importer {CSV, XML}

    public static void main(String args[]) {

        try {
            ArgumentParser parser = ArgumentParsers.newArgumentParser("ABImport")
                    .defaultHelp(true)
                    .description("Import file formats to AuditBucket");

            parser.addArgument("-s", "--server")
                    .setDefault("http://localhost/ab-engine")
                    .help("Host URL to send files to");

            parser.addArgument("files").nargs("*")
                    .help("Path and filename of Audit records to import in the format \"[/filepath/filename.ext],[com.import.YourClass],[batchSize],{skipCount}\"");

            Namespace ns = null;
            try {
                ns = parser.parseArgs(args);
            } catch (ArgumentParserException e) {
                parser.handleError(e);
                System.exit(1);
            }
            List<String> files = ns.getList("files");
            if (files.isEmpty()) {
                parser.handleError(new ArgumentParserException("No files to parse", parser));
                System.exit(1);
            }
            StopWatch watch = new StopWatch();
            watch.start();
            logger.info("*** Starting {}", DateFormat.getDateTimeInstance().format(new Date()));
            long totalRows = 0;
            for (String file : files) {
                int batchSize = 100;
                int skipCount = 0;
                List<String> items = Arrays.asList(file.split("\\s*,\\s*"));
                if (items.size() == 0)
                    parser.handleError(new ArgumentParserException("No file arguments to process", parser));
                int item = 0;
                String fileName = null;
                String fileClass = null;
                for (String itemArg : items) {
                    if (item == 0) {
                        fileName = itemArg;
                    } else if (item == 1) {
                        fileClass = itemArg;
                    } else if (item == 2)
                        batchSize = Integer.parseInt(itemArg);
                    else if (item == 3)
                        skipCount = Integer.parseInt(itemArg);
                    item++;
                }
                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, fileClass, batchSize, skipCount);
                totalRows = totalRows + processFile(ns.get("server").toString(), fileName, Class.forName(fileClass), batchSize, skipCount);
            }
            endProcess(watch, totalRows);

            logger.info("Finished at {}", DateFormat.getDateTimeInstance().format(new Date()));

            //"/Users/mike/Downloads/cow.csv,com.auditbucket.health.Countries,200"

            //"/Users/mike/Desktop/Med/ophthalmology.xml,com.auditbucket.health.medline.study.Ophthalmology,100,-1"
            //"/Users/mike/Dropbox/auditbucket/dataproviders/medline/GraftVsHost.xml,com.auditbucket.health.medline.study.GraftVsHost,100"
            //"/Users/mike/Dropbox/data feeds/Cervical cancer.xml,com.auditbucket.health.medline.study.CervicalCancer,100"
//            "/Users/mike/Dropbox/auditbucket/dataproviders/medicare/HQI_HOSP.csv";com.auditbucket.health.medicare.Hospital;200
            //"/Users/mike/Dropbox/auditbucket/dataproviders/medicare/hvbp_hcahps_02_07_2013.csv";com.auditbucket.health.medicare.Hcahps;500
            //"/Users/mike/downloads/2008_BSA_Inpatient_Claims_PUF.csv";com.auditbucket.health.medicare.InpatientClaims,500

        } catch (Exception e) {
            logger.error("Import error", e);
        }
    }

    static long processFile(String server, String file, Class clazz, int batchSize, int skipCount) throws IllegalAccessException, InstantiationException, IOException, ParserConfigurationException, SAXException, JDOMException, AuditException {
        AbRestClient abExporter = new AbRestClient(server, "mike", "123", batchSize);
        abExporter.setSimulateOnly(batchSize <= 0);

        Mappable mappable = (Mappable) clazz.newInstance();
        //String file = path;
        logger.info("Starting the processing of {}", file);
        try {
            if (mappable.getImporter() == Importer.importer.CSV)
                return processCSVFile(file, abExporter, (DelimitedMappable) mappable, skipCount);
            else if (mappable.getImporter() == Importer.importer.XML)
                return processXMLFile(file, abExporter, (XmlMappable) mappable);

        } finally {
            abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());

        }
        return 0;
    }

    static long processXMLFile(String file, AbRestClient abExporter, XmlMappable mappable) throws ParserConfigurationException, IOException, SAXException, JDOMException, AuditException {
        try {
            long rows = 0;
            StopWatch watch = new StopWatch();
            StreamSource source = new StreamSource(file);
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(source);
            mappable.positionReader(xsr);

            String docType = mappable.getDataType();
            watch.start();
            try {
                while (xsr.getLocalName().equals(docType)) {
                    XmlMappable row = mappable.newInstance();
                    String json = row.setXMLData(xsr);
                    AuditHeaderInputBean header = (AuditHeaderInputBean) row;
                    AuditLogInputBean logInputBean = new AuditLogInputBean("system", new DateTime(header.getWhen()), json);
                    header.setAuditLog(logInputBean);
                    //logger.info(json);
                    xsr.nextTag();
                    writeAudit(abExporter, header, mappable.getClass().getCanonicalName());
                    rows++;
                    if (rows % 500 == 0)
                        logger.info("Processed {} elapsed seconds {}", rows, watch.getTotalTimeSeconds());

                }
            } finally {
                abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());
            }

            return endProcess(watch, rows);


        } catch (XMLStreamException | JAXBException e1) {
            throw new IOException(e1);
        }
    }

    static long processCSVFile(String file, AbRestClient abExporter, DelimitedMappable mappable, int skipCount) throws IOException, IllegalAccessException, InstantiationException, AuditException {

        StopWatch watch = new StopWatch();
        DelimitedMappable row = mappable.newInstance();
        int rows = 0;

        BufferedReader br;
        br = new BufferedReader(new FileReader(file));
        try {
            CSVReader csvReader = new CSVReader(br, row.getDelimiter());

            String[] headerRow = null;
            String[] nextLine;
            if (mappable.hasHeader()) {
                while ((nextLine = csvReader.readNext()) != null) {
                    if (!((nextLine[0].charAt(0) == '#') || nextLine[0].charAt(1) == '#')) {
                        headerRow = nextLine;
                        break;
                    }
                }
            }
            watch.start();
            AbRestClient.type type = mappable.getABType();

            while ((nextLine = csvReader.readNext()) != null) {
                if (!nextLine[0].startsWith("#")) {
                    rows++;
                    if (rows >= skipCount) {
                        if (rows == skipCount)
                            logger.info("Starting to process from row {}", skipCount);
                        row = mappable.newInstance();

                        String jsonData = row.setData(headerRow, nextLine);
                        //logger.info(jsonData);
                        if (type == AbRestClient.type.AUDIT) {
                            AuditHeaderInputBean header = (AuditHeaderInputBean) row;

                            if (!jsonData.equals("")) {
                                jsonData = jsonData.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
                                AuditLogInputBean logInputBean = new AuditLogInputBean("system", new DateTime(), jsonData);
                                header.setAuditLog(logInputBean);
                            } else {
                                // It's all Meta baby - no audit information
                            }
                            writeAudit(abExporter, header, mappable.getClass().getCanonicalName());
                        } else {
                            // Tag
                            TagInputBean tagInputBean = (TagInputBean) row;
                            logger.info(tagInputBean.toString());
                            writeTag(abExporter, tagInputBean, mappable.getClass().getCanonicalName());
                        }
                        if (rows % 500 == 0) {
                            logger.info("Processed {} ", rows);
                        }
                    }
                } else {
                    if (rows % 500 == 0)
                        logger.info("Skipping {} of {}", rows, skipCount);
                }
            }
        } finally {
            abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());
            br.close();
        }

        return endProcess(watch, rows);
    }

    static DecimalFormat f = new DecimalFormat();

    private static long endProcess(StopWatch watch, long rows) {
        watch.stop();
        double mins = watch.getTotalTimeSeconds() / 60;
        logger.info("Processed {} rows in {} secs. rpm = {}", rows, f.format(watch.getTotalTimeSeconds()), f.format(rows / mins));
        return rows;
    }

    private static void writeTag(AbRestClient abExporter, TagInputBean tagInputBean, String message) {
        abExporter.writeTag(tagInputBean, message);
    }

    private static void writeAudit(AbRestClient abExporter, AuditHeaderInputBean auditHeaderInputBean, String message) {
        abExporter.writeAudit(auditHeaderInputBean, message);
    }


}