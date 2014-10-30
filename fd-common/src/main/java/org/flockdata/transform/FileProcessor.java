/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.xml.XmlMappable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    private FdReader defaultStaticDataResolver = null;
    private FdReader staticDataResolver = null; // Instance specific

    public FileProcessor() {

    }

    public FileProcessor(FdReader staticDataResolver) {
        this();
        this.defaultStaticDataResolver = staticDataResolver;
    }

    public Long processFile(ProfileConfiguration importProfile, String file, int skipCount, FdWriter writer, Company company, Boolean async) throws IllegalAccessException, InstantiationException, IOException, FlockException, ClassNotFoundException {
        trackBatcher = new TrackBatcher(importProfile, writer, 100, company, async);
        Mappable mappable = importProfile.getMappable();

        //String file = path;
        logger.info("Starting the processing of {}", file);

        long result = 0;
        try {
            if (importProfile.getContentType() == ProfileConfiguration.ContentType.CSV)
                result = processCSVFile(file, importProfile, skipCount, writer);
            else if (importProfile.getContentType() == ProfileConfiguration.ContentType.XML)
                result = processXMLFile(file, importProfile, writer);
            else if (importProfile.getContentType() == ProfileConfiguration.ContentType.JSON)
                result = processJsonTags(file, importProfile, skipCount, writer);

        } finally {
            if (result > 0) {
                if (mappable != null)
                    trackBatcher.flush();
                else
                    trackBatcher.flush();
            }
        }
        logger.info("Processed {}", file);
        return result;
    }

    private long processJsonTags(String fileName, ProfileConfiguration importProfile, int skipCount, FdWriter restClient) throws FlockException {
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
        return tags.size();  //To change body of created methods use File | Settings | File Templates.
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

            String docType = mappable.getDataType();
            watch.start();
            try {
                long then = new DateTime().getMillis();
                while (xsr.getLocalName().equals(docType)) {

                    XmlMappable row = mappable.newInstance(writer.isSimulateOnly());
                    ContentInputBean contentInputBean = row.setXMLData(xsr, getStaticDataResolver(importProfile, writer));
                    EntityInputBean entityInputBean = (EntityInputBean) row;
                    if (!entityInputBean.getCrossReferences().isEmpty()) {
                        referenceInputBeans.add(new CrossReferenceInputBean(entityInputBean.getFortress(), entityInputBean.getCallerRef(), entityInputBean.getCrossReferences()));
                        rows = rows + entityInputBean.getCrossReferences().size();
                    }
                    if (contentInputBean != null) {
                        if (contentInputBean.getFortressUser() == null)
                            contentInputBean.setFortressUser(importProfile.getFortressUser());
                        entityInputBean.setContent(contentInputBean);
                    }

                    //logger.info(json);
                    xsr.nextTag();
                    trackBatcher.batchEntity(entityInputBean);
                    rows++;
                    if (rows % 500 == 0 && !writer.isSimulateOnly())
                        logger.info("Processed {} elapsed seconds {}", rows, (new DateTime().getMillis() - then) / 1000d);

                }
            } finally {
                trackBatcher.flush();
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

    private long processCSVFile(String file, ProfileConfiguration importProfile, int skipCount, FdWriter writer) throws IOException, IllegalAccessException, InstantiationException, FlockException, ClassNotFoundException {

        StopWatch watch = new StopWatch();
        DelimitedMappable row;
        DelimitedMappable mappable = (DelimitedMappable) importProfile.getMappable();

        int rows = 0;
        Collection<TagInputBean> tags = new ArrayList<>();
        boolean writeToFile = false; // Haven't figured out how to integrate this yet
        // purpose is to write all the tags to an import structure

        BufferedReader br;
        Reader fileObject = getReader(file);

        br = new BufferedReader(fileObject);
        List<CrossReferenceInputBean> referenceInputBeans = new ArrayList<>();

        try {
            CSVReader csvReader = new CSVReader(br, importProfile.getDelimiter());

            String[] headerRow = null;
            String[] nextLine;
            if (mappable.hasHeader()) {
                while ((nextLine = csvReader.readNext()) != null) {
                    if (!((!nextLine[0].equals("") && nextLine[0].charAt(0) == '#'))) {
                        headerRow = nextLine;

                        break;
                    }
                }
            }
            watch.start();
            ProfileConfiguration.DataType DataType = mappable.getABType();

            while ((nextLine = csvReader.readNext()) != null) {
                if (!nextLine[0].startsWith("#")) {
                    rows++;
                    if (rows >= skipCount) {
                        if (rows == skipCount)
                            logger.info("Starting to process from row {}", skipCount);

                        row = (DelimitedMappable) importProfile.getMappable();

                        // ToDo: turn this in to a LogInputBean to reduce impact of interface changes
                        Map<String, Object> jsonData = row.setData(headerRow, nextLine, importProfile, getStaticDataResolver(importProfile, writer));
                        //logger.info(jsonData);
                        if (DataType == ProfileConfiguration.DataType.TRACK) {
                            EntityInputBean entityInputBean = (EntityInputBean) row;

                            if (importProfile.isEntityOnly() || jsonData.isEmpty()) {
                                entityInputBean.setMetaOnly(true);
                                // It's all Meta baby - no log information
                            } else {
                                String updatingUser = entityInputBean.getUpdateUser();
                                if (updatingUser == null)
                                    updatingUser = (entityInputBean.getFortressUser() == null ? importProfile.getFortressUser() : entityInputBean.getFortressUser());

                                ContentInputBean contentInputBean = new ContentInputBean(updatingUser, new DateTime(), jsonData);
                                contentInputBean.setEvent(importProfile.getEvent());
                                entityInputBean.setContent(contentInputBean);
                            }
                            if (!entityInputBean.getCrossReferences().isEmpty()) {
                                referenceInputBeans.add(new CrossReferenceInputBean(entityInputBean.getFortress(), entityInputBean.getDocumentType(), entityInputBean.getCallerRef(), entityInputBean.getCrossReferences()));
                                rows = rows + entityInputBean.getCrossReferences().size();
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
                        if (rows % 500 == 0) {
                            if (!writer.isSimulateOnly())
                                logger.info("Processed {} ", rows);
                        }
                    }
                } else {
                    if (rows % 500 == 0 && !writer.isSimulateOnly())
                        logger.info("Skipping {} of {}", rows, skipCount);
                }
            }
        } finally {
            trackBatcher.flush();
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

        return endProcess(watch, rows);
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
            logger.error("Unable to resolve the file [{}]", file);
            throw new NotFoundException("Unable to resolve the file " + file);
        }
        return fileObject;
    }

    public long endProcess(StopWatch watch, long rows) {
        watch.stop();
        double mins = watch.getTotalTimeSeconds() / 60;
        logger.info("Processed {} rows in {} secs. rpm = {}", rows, formatter.format(watch.getTotalTimeSeconds()), formatter.format(rows / mins));
        return rows;
    }

    public FdReader getStaticDataResolver(ProfileConfiguration importProfile, FdWriter writer) {
        if (staticDataResolver != null)
            return staticDataResolver;

        if (importProfile.getStaticDataClazz() == null)
            return defaultStaticDataResolver;
        else {
            try {
                Constructor<FdReader> constructor = (Constructor<FdReader>) Class.forName(importProfile.getStaticDataClazz()).getConstructor(FdWriter.class);
                staticDataResolver = constructor.newInstance(writer);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                logger.error("Unexpected", e);
            }
            return staticDataResolver;// Unit testing
        }
    }


    public static boolean validateArgs(String pathToBatch) throws NotFoundException, IOException {
        Reader reader = getReader(pathToBatch);
        if ( reader != null )
            reader.close();
        return true;
    }
}
