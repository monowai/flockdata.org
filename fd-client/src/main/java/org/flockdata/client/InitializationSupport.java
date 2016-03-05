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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flockdata.shared.ClientConfiguration;
import org.slf4j.LoggerFactory;

//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;

/**
 * Created by mike on 6/01/16.
 */
public class InitializationSupport {

    static Namespace getConfigureNamespace(String[] args) throws ArgumentParserException {
        net.sourceforge.argparse4j.inf.ArgumentParser parser = ArgumentParsers.newArgumentParser("configure")
                .defaultHelp(true)
                .description("Configures the client for connectivity to FlockData");

        MutuallyExclusiveGroup group = parser.addMutuallyExclusiveGroup();

        group.addArgument("-r", "--reconfig")
                .action(Arguments.storeTrue())
                .required(false)
                .setDefault(false)
                .help("Perform the reconfiguration process");

        group.addArgument("-t", "--test")
                .required(false)
                .setDefault(true)
                .help("Test the stored configuration")
                .action(Arguments.storeTrue());

        parser.addArgument("-x", "--debug")
                .required(false)
                .setDefault(false)
                .help("Debug level logging")
                .action(Arguments.storeTrue());

        parser.addArgument("-c", "--config")
                .required(false)
                .help("Path to fully qualified configuration file");

        parser.addArgument("-cp", "--cpath")
                .setDefault("./conf/")
                .required(false)
                .help("Path to find "+Configure.configFile+"");

        parser.addArgument("-u", "--user")
                .required(false)
                .help("User authorised to create registrations");

        return parser.parseArgs(args);
    }

    public static Namespace getImportNamespace(String args[]) throws ArgumentParserException {
        net.sourceforge.argparse4j.inf.ArgumentParser parser = ArgumentParsers.newArgumentParser("importer")
                .defaultHelp(true)
                .description("Client side batch importer to FlockData");

        parser.addArgument("-"+ ClientConfiguration.KEY_BATCH_SIZE)
                .required(false)
                .help("Default batch size");

        parser.addArgument("-s", "--skip")
                .required(false)
                .help("Start processing from this record");

        parser.addArgument("-x", "--stop")
                .required(false)
                .help("Stop after this many have been processed");

        parser.addArgument("-v", "--validate")
                .required(false)
                .help("Runs a batch and verifies that the entities exist");

        parser.addArgument("-"+ClientConfiguration.AMQP, "--amqp")
                .required(false)
                .setDefault(false)
                .help("Use AMQP instead of HTTP (only works for track requests)");

        parser.addArgument("-c", "--config")
                .required(false)
                .help("Path to fully qualified configuration file");

        parser.addArgument("-cp", "--cpath")
                .setDefault("./conf/")
                .required(false)
                .help("Path to find "+Configure.configFile+"");

        parser.addArgument("files").nargs("*")
                .help("Path and filename of file to import and the import profile in the format \"[/filepath/filename.ext],[/importProfile/profile.json\"");

       return parser.parseArgs(args);
    }

    static org.slf4j.Logger configureLogger(Boolean debug) {
//        Logger.getRootLogger().getLoggerRepository().resetConfiguration();
//        ConsoleAppender console = new ConsoleAppender();
//        String PATTERN = "%m%n";
//        console.setLayout(new PatternLayout(PATTERN));
//        console.setThreshold((debug!=null&& debug ? Level.TRACE : Level.INFO));
//        console.activateOptions();
//
//        //add appender to any Logger (here is root)
//        Logger.getRootLogger().addAppender(console);

        return LoggerFactory.getLogger(Configure.class);
    }

}
