/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.springframework.util.StopWatch;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * General importer with support for CSV and XML parsing. Interacts with AbRestClient to send
 * information via a RESTful interface
 * <p>
 * Will send information to FlockData as either tags or track information.
 * <p>
 * You should extend EntityInputBean or TagInputBean and implement XMLMappable or DelimitedMappable
 * to massage your data prior to dispatch to FD.
 * <p>
 * Parameters:
 * -s=http://localhost:8080
 * <p>
 * quoted string containing "file,DelimitedClass,BatchSize"
 * "./path/to/file/cow.csv,org.flockdata.health.Countries,200"
 * <p>
 * if BatchSize is set to -1, then a simulation only is run; information is not dispatched to the server.
 * This is useful to debug the class implementing Delimited
 *
 * @see org.flockdata.client.rest.FdRestWriter
 * @see org.flockdata.profile.model.Mappable
 * @see TagInputBean
 * @see org.flockdata.track.bean.EntityInputBean
 * <p>
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public class Importer {

    private static  org.slf4j.Logger logger = null;


    public static void main(String args[]) throws ArgumentParserException {

        ClientConfiguration configuration = getConfiguration(args);

        if (configuration.getApiKey() == null) {
            logger.error("No API key is set in the config file. Have you run the config process?");
            System.exit(-1);
        }

        List<String> files = getNameSpace().getList("files");
        if (files.isEmpty()) {
            logger.error("No files to parse!");
            System.exit(1);
        }

        importFiles(configuration, files);
        System.exit(0);
    }

    public static void importFiles(ClientConfiguration configuration, List<String> files){
        StopWatch watch = new StopWatch("Batch Import");
        long totalRows = 0;
        FileProcessor fileProcessor = null;
        try {

            int batchSize = configuration.getBatchSize();

            int skipCount = configuration.getSkipCount();

            int rowsToProcess = configuration.getStopRowProcessCount();

            watch.start();

            for (String thisFile : files) {
                List<String> items = Arrays.asList(thisFile.split("\\s*,\\s*"));

                int item = 0;
                String fileName = null;
                String clazz = null;
                for (String itemArg : items) {
                    if (item == 0) {
                        fileName = itemArg;
                    } else if (item == 1) {
                        clazz = itemArg;
                    }

                    item++;
                }
                ContentProfileImpl contentProfileImpl;
                FdWriter restClient = getRestClient(configuration);
                if (clazz != null) {
                    contentProfileImpl = ProfileReader.getImportProfile(clazz);
                } else {
                    logger.error("No import parameters to work with");
                    return;
                }
                SystemUserResultBean su = restClient.me(); // Use the configured API as the default FU unless another is set
                if ( su == null ) {
                    if ( !configuration.isAmqp())
                        throw new FlockException("Unable to connect to FlockData. Is the service running at [" + configuration.getEngineURL() + "]?");
                    else
                        logger.warn( "Http communications with FlockData are not working. Is the service running at [" + configuration.getEngineURL() + "]?");
                } else if (su.getApiKey() == null)
                    throw new FlockException("Unable to find an API Key in your configuration for the user " + su.getLogin() + ". Have you run the configure process?");

                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, contentProfileImpl, batchSize, skipCount);

                if (fileProcessor == null)
                    fileProcessor = new FileProcessor(skipCount, rowsToProcess);

                // Importer does not know what the company is
                totalRows = totalRows + fileProcessor.processFile(contentProfileImpl, fileName, restClient, null, configuration);
            }
            logger.info("Finished at {}", DateFormat.getDateTimeInstance().format(new Date()));

        } catch (Exception e) {
            logger.error("Import error", e);
            System.exit(-1);
        } finally {
            if (fileProcessor != null)
                fileProcessor.endProcess(watch, totalRows, 0);
        }
    }

    private static FdRestWriter fdClient = null;

    public static FdWriter getRestClient(ClientConfiguration configuration) {
        if ( fdClient !=null)
            return fdClient;

        fdClient = new FdRestWriter(configuration);
        if (!configuration.isAmqp()) {
            String ping = fdClient.ping().toLowerCase();
            if (!ping.startsWith("pong")) {
                logger.warn("Error communicating over http with fd-engine {} ", configuration.getEngineURL());
                if (configuration.isAmqp()) {
                    logger.info("Data can still be sent over AMQP");
                }
            }
        }
        fdClient.setSimulateOnly(configuration.getBatchSize() <= 0);
        return fdClient;

    }

    public static ClientConfiguration getConfiguration(String[] args) throws ArgumentParserException {

        nameSpace = InitializationSupport.getImportNamespace(args);
        logger = InitializationSupport.configureLogger(getNameSpace().getBoolean("debug"));

        File file = Configure.getFile(nameSpace);

        ClientConfiguration importConfig = Configure.getConfiguration(file);

        Object o = nameSpace.get(ClientConfiguration.KEY_BATCH_SIZE);
        if (o != null)
            importConfig.setBatchSize(Integer.parseInt(o.toString().trim()));

        o = nameSpace.get("validate");
        if (o != null)
            importConfig.setValidateOnly(Boolean.parseBoolean(o.toString()));

        o = nameSpace.get(ClientConfiguration.AMQP);
        if (o != null)
            importConfig.setAmqp(Boolean.parseBoolean(o.toString()), true);

        o = nameSpace.get("skip");
        if (o !=null)
            importConfig.setSkipCount(Integer.parseInt(o.toString()));

        o = nameSpace.get("stop");
        if (o !=null)
            importConfig.setStopRowProcessCount(Integer.parseInt(o.toString()));

        return importConfig;
    }

    private static Namespace getNameSpace() {
        return nameSpace;
    }

    private static Namespace nameSpace = null;


}