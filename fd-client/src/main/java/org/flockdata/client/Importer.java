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

package org.flockdata.client;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flockdata.client.rest.FdRestReader;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * General importer with support for CSV and XML parsing. Interacts with AbRestClient to send
 * information via a RESTful interface
 * <p/>
 * Will send information to FlockData as either tags or track information.
 * <p/>
 * You should extend EntityInputBean or TagInputBean and implement XMLMappable or DelimitedMappable
 * to massage your data prior to dispatch to FD.
 * <p/>
 * Parameters:
 * -s=http://localhost:8080/fd-engine
 * <p/>
 * quoted string containing "file,DelimitedClass,BatchSize"
 * "./path/to/file/cow.csv,org.flockdata.health.Countries,200"
 * <p/>
 * if BatchSize is set to -1, then a simulation only is run; information is not dispatched to the server.
 * This is useful to debug the class implementing Delimited
 *
 * @see org.flockdata.client.rest.FdRestWriter
 * @see org.flockdata.profile.model.Mappable
 * @see org.flockdata.registration.bean.TagInputBean
 * @see org.flockdata.track.bean.EntityInputBean
 * <p/>
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public class Importer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Importer.class);

    private static Namespace getCommandLineArgs(String args[]) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("importer")
                .defaultHelp(true)
                .description("Client side batch importer to FlockData");

        parser.addArgument("-b", "--batch")
                .required(false)
                .help("Default batch size");

        parser.addArgument("-v", "--validate")
                .required(false)
                .help("Runs a batch and verifies that the entities exist");

        parser.addArgument("-a", "--async")
                .required(false)
                .setDefault(false)
                .help("Async processing");

        parser.addArgument("-amqp", "--amqp")
                .required(false)
                .setDefault(false)
                .help("Use AMQP instead of HTTP (experimental)");

        parser.addArgument("-c", "--path")
                .setDefault("./conf")
                .required(false)
                .help("Configuration file path");

        parser.addArgument("files").nargs("*")
                .help("Path and filename of file to import and the import profile in the format \"[/filepath/filename.ext],[/importProfile/profile.json\"");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        return ns;

    }

    public static void main(String args[]) {
        StopWatch watch = new StopWatch("Batch Import");
        long totalRows = 0;
        FileProcessor fileProcessor = null;
        try {
            Namespace ns = getCommandLineArgs(args);
            File file = Configure.getFile(Configure.configFile, ns);
            ClientConfiguration configuration = Configure.readConfiguration(file);
            if (configuration.getApiKey() == null) {
                logger.error("No API key is set in the config file. Have you run the config process?");
                System.exit(-1);
            }

            List<String> files = ns.getList("files");
            if (files.isEmpty()) {
                logger.error("No files to parse!");
                System.exit(1);
            }
            String batch = ns.getString("batch");

            int batchSize = configuration.getBatchSize();
            if (batch != null && !batch.equals(""))
                configuration.setBatchSize(Integer.parseInt(batch));

            Object o = ns.get("async");
            if ( o!=null )
                configuration.setAsync(Boolean.parseBoolean(o.toString()));

            o = ns.get("validate");
            if ( o!=null )
                configuration.setValidateOnly(Boolean.parseBoolean(o.toString()));

            o = ns.get("amqp");
            if ( o!=null )
                configuration.setAmqp(Boolean.parseBoolean(o.toString()));


            watch.start();
            //logger.info("*** Starting {}", DateFormat.getDateTimeInstance().format(new Date()));

            for (String thisFile : files) {

                int skipCount = 0;
                List<String> items = Arrays.asList(thisFile.split("\\s*,\\s*"));

                int item = 0;
                String fileName = null;
                String clazz = null;
                for (String itemArg : items) {
                    if (item == 0) {
                        fileName = itemArg;
                    } else if (item == 1) {
                        clazz = itemArg;
                    } else if (item == 2)
                        skipCount = Integer.parseInt(itemArg);

                    item++;
                }
                ImportProfile importProfile;
                FdWriter restClient = getRestClient(configuration);
                if (clazz != null) {
                    //importParams = Class.forName(importProfile);

                    importProfile = ClientConfiguration.getImportParams(clazz);

                } else {
                    logger.error("No import parameters to work with");
                    return;
                }
                SystemUserResultBean su = restClient.me(); // Use the configured API as the default FU unless another is set
                if (su != null) {
                    importProfile.setFortressUser(su.getLogin());
                } else {
                    logger.error("Unable to validate the system user as a default fortress user. This will cause errors in the TrackEP if you do not set the FortressUser");
                }
                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, importProfile, batchSize, skipCount);


                if (fileProcessor== null )
                    fileProcessor = new FileProcessor(new FdRestReader(restClient));

                // Importer does not know what the company is
                totalRows = totalRows + fileProcessor.processFile(importProfile, fileName, skipCount, restClient, null, configuration);
            }
            logger.info("Finished at {}", DateFormat.getDateTimeInstance().format(new Date()));

        } catch (Exception e) {
            logger.error("Import error", e);
            System.exit(-1);
        } finally {
            if ( fileProcessor!=null)
                fileProcessor.endProcess(watch, totalRows);


        }
        System.exit(0);
    }


    private static FdWriter getRestClient(ClientConfiguration configuration) {
        FdRestWriter fdClient = new FdRestWriter(configuration);
        String ping = fdClient.ping();
        if (!ping.equalsIgnoreCase("pong!")) {
            logger.error("Error communicating with fd-engine");
        }
        boolean simulateOnly = configuration.getBatchSize() <= 0;
        fdClient.setSimulateOnly(simulateOnly);
        return fdClient;

    }



}