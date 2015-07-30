/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform;

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.model.Company;
import org.flockdata.transform.json.JsonEntityMapper;
import org.flockdata.transform.xml.XmlMappable;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StopWatch;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:29 PM
 */
public class FileProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private static final DecimalFormat formatter = new DecimalFormat();
    private TrackBatcher trackBatcher;
    private long skipCount, rowsToProcess = 0;

    public FileProcessor() {

    }

    public FileProcessor(int skipCount, int rowsToProcess) {
        this.skipCount = skipCount;
        this.rowsToProcess = rowsToProcess;
    }

    public Collection<String> resolveFiles(String source) throws IOException, NotFoundException {
        ArrayList<String> results = new ArrayList<>();
        boolean absoluteFile = true;
        if (source.contains("*") || source.contains("?") || source.endsWith(File.separator))
            absoluteFile = false;

        if (absoluteFile) {
            Reader reader;
            reader = getReader(source);
            if (reader != null) {
                reader.close();
                results.add(source);
                return results;
            }
        }

        String filter;
        String path;

        Path toResolve = Paths.get(source);

        if (source.endsWith(File.separator))
            filter = "*";
        else
            filter = toResolve.getFileName().toString();
        if (filter == null)
            filter = "*";
        path = source.substring(0, source.lastIndexOf(File.separator) + 1);
        FileFilter fileFilter = new WildcardFileFilter(filter);

        // Split the source in to path and filter.
        //path = source.substring(0, source.indexOf())

        File folder = new UrlResource("file:" + path).getFile();
        File[] listOfFiles = folder.listFiles(fileFilter);
        if (listOfFiles == null) {
            folder = new ClassPathResource(path).getFile();
            listOfFiles = folder.listFiles(fileFilter);
        }

        for (File file : listOfFiles) {
            results.add(file.toString());
        }


        return results;
    }

    public Long processFile(ProfileConfiguration importProfile, String source, FdWriter writer, Company company, ClientConfiguration defaults) throws IllegalAccessException, InstantiationException, IOException, FlockException, ClassNotFoundException {

        trackBatcher = new TrackBatcher(importProfile, writer, defaults, company);
        //Mappable mappable = importProfile.getMappable();

        //String source = path;
        logger.info("Start processing of {}", source);
        Collection<String> files = resolveFiles(source);
        long result = 0;
        try {
            for (String file : files) {

                if (importProfile.getContentType() == ProfileConfiguration.ContentType.CSV)
                    result = processCSVFile(file, importProfile, writer);
                else if (importProfile.getContentType() == ProfileConfiguration.ContentType.XML)
                    result = processXMLFile(file, importProfile, writer);
                else if (importProfile.getContentType() == ProfileConfiguration.ContentType.JSON) {
                    if (importProfile.getTagOrEntity() == ProfileConfiguration.DataType.ENTITY)
                        result = processJsonEntities(file, importProfile, writer);
                    else
                        result = processJsonTags(file, importProfile, writer);
                }

            }
        } finally {
            if (result > 0) {
                writer.close(trackBatcher);
            }
        }

        logger.info("Processed {}", source);
        return result;
    }

    private long processJsonTags(String fileName, ProfileConfiguration importProfile, FdWriter restClient) throws FlockException {
        Collection<TagInputBean> tags;
        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        long processed = 0;
        try {
            File file = new File(fileName);
            InputStream stream = null;
            if (!file.exists()) {
                // Try as a resource
                stream = ClassLoader.class.getResourceAsStream(fileName);
                if (stream == null) {
                    logger.error("{} does not exist", fileName);
                    return 0;
                }
            }
            TypeFactory typeFactory = mapper.getTypeFactory();
            CollectionType collType = typeFactory.constructCollectionType(ArrayList.class, TagInputBean.class);

            if (file.exists())
                tags = mapper.readValue(file, collType);
            else
                tags = mapper.readValue(stream, collType);
            for (TagInputBean tag : tags) {
                trackBatcher.batchTag(tag, "JSON Tag Importer");
                processed++;
            }

        } catch (IOException e) {
            logger.error("Error writing exceptions with {} [{}]", fileName, e.getMessage());
            throw new RuntimeException("IO Exception ", e);
        } finally {
            if (processed > 0l)
                trackBatcher.flush();

        }
        return tags.size();
    }

    private long processJsonEntities(String fileName, ProfileConfiguration importProfile, FdWriter writer) throws FlockException {
        long rows = 0;

        File file = new File(fileName);
        InputStream stream = null;
        if (!file.exists()) {
            // Try as a resource
            stream = ClassLoader.class.getResourceAsStream(fileName);
            if (stream == null) {
                logger.error("{} does not exist", fileName);
                return 0;
            }
        }
        StopWatch watch = new StopWatch();

        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser;
        List<CrossReferenceInputBean> referenceInputBeans = new ArrayList<>();

        try {

            //String docType = mappable.getDataType();
            watch.start();
            ObjectMapper om = new ObjectMapper();
            try {

                if (stream != null)
                    jParser = jfactory.createParser(stream);
                else
                    jParser = jfactory.createParser(file);


                JsonToken currentToken = jParser.nextToken();
                long then = new DateTime().getMillis();
                JsonNode node;
                if (currentToken == JsonToken.START_ARRAY || currentToken == JsonToken.START_OBJECT) {
                    while (currentToken != null && currentToken != JsonToken.END_OBJECT) {

                        while (currentToken != null && jParser.nextToken() != JsonToken.END_ARRAY) {
                            node = om.readTree(jParser);
                            if (node != null) {
                                processJsonNode(node, importProfile, writer, referenceInputBeans);
                                if (stopProcessing(rows++, then)) {
                                    break;
                                }

                            }
                            currentToken = jParser.nextToken();

                        }
                    }
                } else if (currentToken == JsonToken.START_OBJECT) {

                    //om.readTree(jParser);
                    node = om.readTree(jParser);
                    processJsonNode(node, importProfile, writer, referenceInputBeans);
                }

            } catch (IOException e1) {
                logger.error("Unexpected", e1);
            }


        } finally {
            writer.close(trackBatcher);
        }
        if (!referenceInputBeans.isEmpty()) {
            logger.debug("Wrote [{}] cross references",
                    writeCrossReferences(writer, referenceInputBeans));
        }

        return endProcess(watch, rows);
    }

    private void processJsonNode(JsonNode node, ProfileConfiguration importProfile, FdWriter writer, List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        JsonEntityMapper entityInputBean = new JsonEntityMapper();
        entityInputBean.setData(node, importProfile);
        if (entityInputBean.getFortress() == null)
            entityInputBean.setFortress(importProfile.getFortressName());

        if (!entityInputBean.getCrossReferences().isEmpty()) {
            referenceInputBeans.add(new CrossReferenceInputBean(entityInputBean));
            entityInputBean.getCrossReferences().size();
        }

        ContentInputBean contentInputBean = new ContentInputBean();
        if (contentInputBean.getFortressUser() == null)
            contentInputBean.setFortressUser(importProfile.getFortressUser());
        entityInputBean.setContent(contentInputBean);
        contentInputBean.setWhat(FlockDataJsonFactory.getObjectMapper().convertValue(node, Map.class));

        trackBatcher.batchEntity(entityInputBean);

    }

    private long processXMLFile(String file, ProfileConfiguration importProfile, FdWriter writer) throws IOException, FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        try {
            long rows = 0;
            XmlMappable mappable = (XmlMappable) importProfile.getMappable();
            StopWatch watch = new StopWatch();
            StreamSource source = new StreamSource(file);
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(source);
            mappable.positionReader(xsr);
            List<CrossReferenceInputBean> referenceInputBeans = new ArrayList<>();

            String dataType = mappable.getDataType();
            watch.start();
            try {
                long then = new DateTime().getMillis();
                while (xsr.getLocalName().equals(dataType)) {

                    XmlMappable row = mappable.newInstance(importProfile);
                    ContentInputBean contentInputBean = row.setXMLData(xsr, importProfile);
                    EntityInputBean entityInputBean = (EntityInputBean) row;

                    if (entityInputBean.getFortress() == null)
                        entityInputBean.setFortress(importProfile.getFortressName());

                    if (entityInputBean.getFortressUser() == null)
                        entityInputBean.setFortressUser(importProfile.getFortressUser());

                    if (!entityInputBean.getCrossReferences().isEmpty()) {
                        referenceInputBeans.add(new CrossReferenceInputBean(entityInputBean));
                        entityInputBean.getCrossReferences().size();
                    }
                    if (contentInputBean != null) {
                        if (contentInputBean.getFortressUser() == null)
                            contentInputBean.setFortressUser(importProfile.getFortressUser());
                        entityInputBean.setContent(contentInputBean);
                    }
                    rows++;
                    xsr.nextTag();
                    trackBatcher.batchEntity(entityInputBean);

                    if (stopProcessing(rows, then)) {
                        break;
                    }

                }
            } finally {
                writer.close(trackBatcher);
            }
            if (!referenceInputBeans.isEmpty()) {
                logger.debug("Wrote [{}] cross references", writeCrossReferences(writer, referenceInputBeans));
            }
            return endProcess(watch, rows);


        } catch (XMLStreamException | JAXBException e1) {
            throw new IOException(e1);
        }
    }

    private int writeCrossReferences(FdWriter fdWriter, List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        return fdWriter.flushXReferences(referenceInputBeans);
    }

    private long processCSVFile(String file, ProfileConfiguration importProfile, FdWriter writer) throws IOException, IllegalAccessException, InstantiationException, FlockException, ClassNotFoundException {

        StopWatch watch = new StopWatch();
        DelimitedMappable row;
        DelimitedMappable mappable = (DelimitedMappable) importProfile.getMappable();

        long currentRow = 0;
        Collection<TagInputBean> tags = new ArrayList<>();
        boolean writeToFile = false; // Haven't figured out how to integrate this yet
        // purpose is to write all the tags to an import structure

        BufferedReader br;
        Reader fileObject = getReader(file);

        br = new BufferedReader(fileObject);
        List<CrossReferenceInputBean> referenceInputBeans = new ArrayList<>();

        try {
            CSVReader csvReader;
            if (importProfile.getQuoteCharacter() != null)
                csvReader = new CSVReader(br, importProfile.getDelimiter(), importProfile.getQuoteCharacter().charAt(0));
            else
                csvReader = new CSVReader(br, importProfile.getDelimiter());

            String[] headerRow = null;
            String[] nextLine;
            if (importProfile.hasHeader()) {
                while ((nextLine = csvReader.readNext()) != null) {
                    if (!nextLine[0].equals("")) {
                        if (!(((nextLine[0].charAt(0) == '#') || nextLine[0].charAt(1) == '#'))) {
                            headerRow = nextLine;
                            break;
                        }
                    }
                }
            }
            watch.start();
            ProfileConfiguration.DataType DataType = importProfile.getTagOrEntity();
            if (skipCount > 0)
                logger.info("Skipping first {} rows", skipCount);

            long then = System.currentTimeMillis();

            while ((nextLine = csvReader.readNext()) != null) {
                if (!ignoreRow(nextLine)) {
                    if (headerRow == null) {
                        headerRow = TransformationHelper.defaultHeader(nextLine, importProfile);
                    }
                    currentRow++;
                    if (currentRow >= skipCount) {
                        if (currentRow == skipCount)
                            logger.info("Processing now begins at row {}", skipCount);

                        row = (DelimitedMappable) importProfile.getMappable();
                        nextLine = preProcess(nextLine, importProfile);
                        // ToDo: turn this in to a LogInputBean to reduce impact of interface changes
                        Map<String, Object> jsonData = row.setData(headerRow, nextLine, importProfile);
                        if ( jsonData!=null ) {
                            if (DataType == ProfileConfiguration.DataType.ENTITY) {
                                EntityInputBean entityInputBean = (EntityInputBean) row;

                                if (importProfile.isEntityOnly() || jsonData.isEmpty()) {
                                    entityInputBean.setEntityOnly(true);
                                    // It's all Meta baby - no log information
                                } else {
                                    String updatingUser = entityInputBean.getUpdateUser();
                                    if (updatingUser == null)
                                        updatingUser = (entityInputBean.getFortressUser() == null ? importProfile.getFortressUser() : entityInputBean.getFortressUser());

                                    ContentInputBean contentInputBean = new ContentInputBean(updatingUser, (entityInputBean.getWhen() != null ? new DateTime(entityInputBean.getWhen()) : null), jsonData);
                                    contentInputBean.setEvent(importProfile.getEvent());
                                    entityInputBean.setContent(contentInputBean);
                                }
                                if (!entityInputBean.getCrossReferences().isEmpty()) {
                                    referenceInputBeans.add(new CrossReferenceInputBean(entityInputBean));
                                    currentRow = currentRow + entityInputBean.getCrossReferences().size();
                                }

                                trackBatcher.batchEntity(entityInputBean);
                            } else {// Tag
                                if (!jsonData.isEmpty()) {
                                    TagInputBean tagInputBean = (TagInputBean) row;

                                    if (writeToFile)
                                        tags.add(tagInputBean);
                                    else
                                        trackBatcher.batchTag(tagInputBean, mappable.getClass().getCanonicalName());
                                }
                            }
                            if (stopProcessing(currentRow, then)) {
                                break;
                            }
                        }


                    }
                }
            }
        } finally {

            writer.close(trackBatcher);

            if (!referenceInputBeans.isEmpty()) {
                // ToDo: This approach is un-scalable - routine works but the ArrayList is kept in memory. It's ok for now...
                logger.debug("Wrote [{}] cross references", writeCrossReferences(writer, referenceInputBeans));
            }
            br.close();
        }
        if (writeToFile) {
            // ToDo: Unsure how to handle this. CSV -> JSON
            ObjectMapper om = FlockDataJsonFactory.getObjectMapper();
            try {
                om.writerWithDefaultPrettyPrinter().writeValue(new File(file + ".json"), tags);
            } catch (IOException e) {
                logger.error("Error writing exceptions", e);
            }
        }

        return endProcess(watch, currentRow);
    }

    public boolean stopProcessing(long currentRow) {
        return stopProcessing(currentRow, 0l);
    }

    public boolean stopProcessing(long currentRow, long then) {
        //DAT-350

        if (rowsToProcess == 0) {
            if (currentRow % 1000 == 0)
                logger.info("Processed {} elapsed seconds {}", currentRow - skipCount, (new DateTime().getMillis() - then) / 1000d);
            return false;
        }
        boolean stop = currentRow >= skipCount + rowsToProcess;

        if (!stop && currentRow != skipCount && then > 0 && currentRow % 1000 == 0)
            logger.info("Processed {} elapsed seconds {}", currentRow - skipCount, (new DateTime().getMillis() - then) / 1000d);

        if (currentRow <= skipCount)
            return false;

        if (stop)
            logger.info("Process stopping after the {} requested rows.", rowsToProcess);

        return stop;
    }

    private boolean ignoreRow(String[] nextLine) {
        return nextLine[0].startsWith("#");
    }
    static StandardEvaluationContext context = new StandardEvaluationContext();

    private String[] preProcess(String[] row, ProfileConfiguration importProfile) {
        String[] result = new String[row.length];
        String exp = importProfile.getPreParseRowExp();
        if ((exp == null || exp.equals("")))
            return row;
        int i = 0;
        for (String column : row) {

            Object value = evaluateExpression(column, exp);
            result[i] = value.toString();
            i++;


        }
        return result;
    }

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static Object evaluateExpression(Object value, String expression) {
        context.setVariable("value", value);
        return parser.parseExpression(expression).getValue(context);
    }

    private static Reader getReader(String file) throws NotFoundException {
        InputStream stream = ClassLoader.class.getResourceAsStream(file);

        Reader fileObject = null;
        try {
            fileObject = new FileReader(file);
        } catch (FileNotFoundException e) {
            if (stream != null)
                fileObject = new InputStreamReader(stream);

        }
        if (fileObject == null) {
            logger.error("Unable to resolve the source [{}]", file);
            throw new NotFoundException("Unable to resolve the source " + file);
        }
        return fileObject;
    }

    public long endProcess(StopWatch watch, long rows) {
        watch.stop();
        double mins = watch.getTotalTimeSeconds() / 60;
        long rowsProcessed = rows - skipCount;
        if (skipCount > 0)
            logger.info("Completed {} rows in {} secs. rpm = {}. Skipped first {} rows. Finished on row {}", rowsProcessed, formatter.format(watch.getTotalTimeSeconds()), formatter.format(rowsProcessed / mins), skipCount, rows);
        else
            logger.info("Completed {} rows in {} secs. rpm = {}", rowsProcessed, formatter.format(watch.getTotalTimeSeconds()), formatter.format(rowsProcessed / mins), rows);
        return rows;
    }

    public static boolean validateArgs(String pathToBatch) throws NotFoundException, IOException {
        Reader reader = getReader(pathToBatch);
        if (reader != null)
            reader.close();
        return true;
    }
}
