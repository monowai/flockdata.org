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

import org.flockdata.client.commands.Login;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.shared.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
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
@Profile("fd-importer")
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.flockdata.authentication", "org.flockdata.shared", "org.flockdata.client"})
public class Importer  {

    private Logger logger = LoggerFactory.getLogger(Importer.class);

    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FdRestWriter fdClient;

    @Autowired
    private FileProcessor fileProcessor;

    @Value("${auth.user:#{null}}")
    String authUser;

    @PostConstruct
    void importFiles() {
        logger.info("Looking for Flockdata on {}", clientConfiguration.getServiceUrl());
        if ( authUser!=null ){

            String[] uArgs = authUser.split(":");
            clientConfiguration.setHttpUser(uArgs[0]);
            clientConfiguration.setHttpPass(uArgs[1]);
            Login login = new Login(clientConfiguration,fdClient);
            login.exec();
            if (login.error()!= null){
                logger.error(login.error());
                System.exit(-1);
            }
            clientConfiguration.setApiKey(login.result().getApiKey());

        }

        if (clientConfiguration.getApiKey() == null) {
            logger.error("No API key is set in the config file. Have you run the config process?");
            System.exit(-1);
        }

        Collection<String> files = clientConfiguration.getFilesToImport();
        if (files.isEmpty()) {
            logger.error("No files to parse!");
            System.exit(1);
        }

        StopWatch watch = new StopWatch("Batch Import");
        int totalRows = 0;
        try {

            int batchSize = clientConfiguration.getBatchSize();

            int skipCount = clientConfiguration.getSkipCount();

            int rowsToProcess = clientConfiguration.getStopRowProcessCount();

            watch.start();

            for (String thisFile : clientConfiguration.getFilesToImport()) {
                List<String> items = Arrays.asList(thisFile.split("\\s*,\\s*"));

                int item = 0;
                String fileName = null;
                String profile = null;
                for (String itemArg : items) {
                    if (item == 0) {
                        fileName = itemArg;
                    } else if (item == 1) {
                        profile = itemArg;
                    }

                    item++;
                }
                ContentProfileImpl contentProfileImpl;
                if (fdClient != null && profile != null) {
                    contentProfileImpl = ProfileReader.getImportProfile(profile);
                } else {
                    logger.error("No import parameters to work with");
                    return;
                }
                SystemUserResultBean su = fdClient.me(); // Use the configured API as the default FU unless another is set
                if (su == null) {
                    if (!clientConfiguration.isAmqp())
                        throw new FlockException("Unable to connect to FlockData. Is the service running at [" + clientConfiguration.getServiceUrl() + "]?");
                    else
                        logger.warn("Http communications with FlockData are not working. Is the service running at [" + clientConfiguration.getServiceUrl() + "]?");
                } else if (su.getApiKey() == null)
                    throw new FlockException("Unable to find an API Key in your configuration for the user " + su.getLogin() + ". Have you run the configure process?");

                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, contentProfileImpl, batchSize, skipCount);

                // Importer does not know what the company is
                totalRows = totalRows + fileProcessor.processFile(contentProfileImpl, fileName);
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


}