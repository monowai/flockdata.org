/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.client;

import au.com.bytecode.opencsv.CSVReader;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;

/**
 * General importer with support for CSV and XML parsing. Interacts with AbRestClient to send
 * information via a RESTful interface
 * <p/>
 * Will send information to AuditBucket as either tags or track information.
 * <p/>
 * You should extend MetaInputBean or TagInputBean and implement XMLMappable or DelimitedMappable
 * to massage your data prior to dispatch to AB.
 * <p/>
 * Parameters:
 * -s=http://localhost:8080/ab-engine
 * <p/>
 * quoted string containing "file,DelimitedClass,BatchSize"
 * "./path/to/file/cow.csv,com.auditbucket.health.Countries,200"
 * <p/>
 * if BatchSize is set to -1, then a simulation only is run; information is not dispatched to the server.
 * This is useful to debug the class implementing Delimited
 *
 * @see AbRestClient
 * @see Mappable
 * @see TagInputBean
 * @see com.auditbucket.track.bean.MetaInputBean
 *      <p/>
 *      User: Mike Holdsworth
 *      Since: 13/10/13
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

            parser.addArgument("-b", "--batch")
                    .setDefault(100)
                    .help("Default batch size");

            parser.addArgument("-x", "--xref")
                    .setDefault(false)
                    .help("Cross References Only");

            parser.addArgument("files").nargs("*")
                     .help("Path and filename of Audit records to import in the format \"[/filepath/filename.ext],[com.import.YourClass],{skipCount}\"");

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
            String b = ns.getString("batch");
            int batchSize = 100;
            if ( b !=null && !"".equals(b))
                batchSize = Integer.parseInt(b);


            StopWatch watch = new StopWatch();
            watch.start();
            logger.info("*** Starting {}", DateFormat.getDateTimeInstance().format(new Date()));
            long totalRows = 0;
            for (String file : files) {

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
                        skipCount = Integer.parseInt(itemArg);

                    item++;
                }
                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, fileClass, batchSize, skipCount);
                totalRows = totalRows + processFile(ns.get("server").toString(), fileName, (fileClass!=null ?Class.forName(fileClass):null), batchSize, skipCount);
            }
            endProcess(watch, totalRows);

            logger.info("Finished at {}", DateFormat.getDateTimeInstance().format(new Date()));

        } catch (Exception e) {
            logger.error("Import error", e);
        }
    }

    static long processFile(String server, String file, Class clazz, int batchSize, int skipCount) throws IllegalAccessException, InstantiationException, IOException, ParserConfigurationException, SAXException, JDOMException, DatagioException {
        AbRestClient abExporter = new AbRestClient(server, "mike", "123", batchSize);
        boolean simulateOnly = batchSize <= 0;
        abExporter.setSimulateOnly(simulateOnly);
        Mappable mappable =null;

        if (clazz !=null ){
            mappable =(Mappable) clazz.newInstance();
        }

        //String file = path;
        logger.info("Starting the processing of {}", file);
        try {
            if ( clazz == null )
                return processJsonTags(file, abExporter, skipCount, simulateOnly);
            else if (mappable.getImporter() == Importer.importer.CSV)
                return processCSVFile(file, abExporter, (DelimitedMappable) mappable, skipCount, simulateOnly );
            else if (mappable.getImporter() == Importer.importer.XML)
                return processXMLFile(file, abExporter, (XmlMappable) mappable, simulateOnly);

        } finally {
            if ( mappable != null )
                abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());
            else
                abExporter.flush("Tags", AbRestClient.type.TAG);

        }
        return 0;
    }

    private static long processJsonTags(String fileName, AbRestClient abExporter, int skipCount, boolean simulateOnly) {
        Collection<TagInputBean> tags;
            ObjectMapper mapper = new ObjectMapper();
            try {
                File file = new File(fileName);
                if (!file.exists()) {
                    logger.error("{} does not exist", fileName);
                    return 0;
                }
                TypeFactory typeFactory = mapper.getTypeFactory();
                CollectionType collType = typeFactory.constructCollectionType(ArrayList.class, TagInputBean.class);

                tags = mapper.readValue(file, collType);
                for (TagInputBean tag : tags) {
                    abExporter.writeTag(tag, "JSON Tag Importer");
                }
            } catch (IOException e) {
                logger.error("Error writing exceptions with {} [{}]",fileName, e.getMessage());
                throw new RuntimeException("IO Exception ", e);
            } finally {
                abExporter.flush("Finishing processing of TagInputBeans " +fileName);

            }
        return tags.size();  //To change body of created methods use File | Settings | File Templates.
    }

    static long processXMLFile(String file, AbRestClient abExporter, XmlMappable mappable, boolean simulateOnly) throws ParserConfigurationException, IOException, SAXException, JDOMException, DatagioException {
        try {
            long rows = 0;
            StopWatch watch = new StopWatch();
            StreamSource source = new StreamSource(file);
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(source);
            mappable.positionReader(xsr);
            List<CrossReferenceInputBean> referenceInputBeans= new ArrayList<>();

            String docType = mappable.getDataType();
            watch.start();
            try {
                long then = new DateTime().getMillis();
                while (xsr.getLocalName().equals(docType)) {
                    XmlMappable row = mappable.newInstance(simulateOnly);
                    String json = row.setXMLData(xsr, abExporter);
                    MetaInputBean header = (MetaInputBean) row;
                    if ( !header.getCrossReferences().isEmpty()){
                        referenceInputBeans.add(new CrossReferenceInputBean(header.getFortress(),header.getCallerRef(),header.getCrossReferences()));
                        rows = rows + header.getCrossReferences().size();
                    }
                    LogInputBean logInputBean = new LogInputBean("system", new DateTime(header.getWhen()), json);
                    header.setLog(logInputBean);
                    //logger.info(json);
                    xsr.nextTag();
                    writeAudit(abExporter, header, mappable.getClass().getCanonicalName());
                    rows++;
                    if (rows % 500 == 0 && !simulateOnly)
                        logger.info("Processed {} elapsed seconds {}", rows, new DateTime().getMillis()-then /1000d);

                }
            } finally {
                abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());
            }
            if ( ! referenceInputBeans.isEmpty()){
                logger.debug ("Wrote [{}] cross references",writeCrossReferences( abExporter, referenceInputBeans, "Cross References"));
            }
            return endProcess(watch, rows);


        } catch (XMLStreamException | JAXBException e1) {
            throw new IOException(e1);
        }
    }

    private static int writeCrossReferences(AbRestClient abExporter, List<CrossReferenceInputBean> referenceInputBeans, String message) {
        return abExporter.flushXReferences(referenceInputBeans);
    }

    static long processCSVFile(String file, AbRestClient abExporter, DelimitedMappable mappable, int skipCount, boolean simulateOnly) throws IOException, IllegalAccessException, InstantiationException, DatagioException {

        StopWatch watch = new StopWatch();
        DelimitedMappable row = mappable.newInstance(simulateOnly);
        int rows = 0;
        Collection<TagInputBean>tags = new ArrayList<>();
        boolean writeToFile = true; // Haven't figured out how to integrate this yet
                                     // purpose is to write all the tags to an import stucture

        BufferedReader br;
        FileReader fileObject = new FileReader(file);
        br = new BufferedReader(fileObject);
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
                        row = mappable.newInstance(simulateOnly);

                        String jsonData = row.setData(headerRow, nextLine, abExporter);
                        //logger.info(jsonData);
                        if (type == AbRestClient.type.AUDIT) {
                            MetaInputBean header = (MetaInputBean) row;

                            if (!"".equals(jsonData)) {
                                jsonData = jsonData.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
                                LogInputBean logInputBean = new LogInputBean("system", new DateTime(), jsonData);
                                header.setLog(logInputBean);
                            } else {
                                // It's all Meta baby - no track information
                            }
                            writeAudit(abExporter, header, mappable.getClass().getCanonicalName());
                        } else {// Tag
                            if (!"".equals(jsonData)) {
                                TagInputBean tagInputBean = (TagInputBean) row;

                                if ( writeToFile )
                                    tags.add(tagInputBean);
                                else
                                    writeTag(abExporter, tagInputBean, mappable.getClass().getCanonicalName());
                            }
                        }
                        if (rows % 500 == 0) {
                            if ( !simulateOnly)
                                logger.info("Processed {} ", rows);
                        }
                    }
                } else {
                    if (rows % 500 == 0 && !simulateOnly)
                        logger.info("Skipping {} of {}", rows, skipCount);
                }
            }
        } finally {
            abExporter.flush(mappable.getClass().getCanonicalName(), mappable.getABType());
            br.close();
        }
        if ( writeToFile){
            ObjectMapper om = new ObjectMapper();
            try {

                om.writerWithDefaultPrettyPrinter().writeValue(new File( file+".json"), tags);
            } catch (IOException e) {
                logger.error("Error writing exceptions", e);
            }
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

    private static void writeAudit(AbRestClient abExporter, MetaInputBean metaInputBean, String message) {
        abExporter.writeAudit(metaInputBean, message);
    }


}