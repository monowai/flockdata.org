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

import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.bean.SystemUserResultBean;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 12:42 PM
 */
public class Configure {
    private static org.slf4j.Logger logger;
    static String configFile = "client.config";
    static String internalUser = null;
    static String internalPass = null;

    public static void main(String args[]) {

        Namespace ns = getCommandLineArgs(args);
        internalUser = ns.getString("user");

        File file = getFile(configFile, ns);
        ClientConfiguration defaults = readConfiguration(file);
        configureLogger(ns.getBoolean("debug"));

        boolean reconfigure = ns.getBoolean("reconfig");

        if (!file.exists() || reconfigure) {

            logger.info("** {}configuration process", (reconfigure ? "re" : "initial "));
            String engineURL = defaults.getEngineURL();
            logger.info("** Looking for FlockData on [{}]", engineURL);
            pingServer(defaults, engineURL);
            defaults.setApiKey(null);

            String version = getVersion(engineURL, defaults.getApiKey());

            if (version != null) {
                logger.info("** Success!! All configured default settings are working! Talking to FlockData version {}", version);
                if (reconfigure) {
                    Boolean yes = getBooleanValue("Reconfigure? ", "Do you still wish to reconfigure your settings? (n)", "n");
                    if (yes) {
                        configure(file, defaults);
                    }
                } else {
                    writeConfiguration(file, defaults);
                }

            } else {
                configure(file, defaults);
            }
        }
        // Test the configuration with the defaults provided
        if (ns.getBoolean("test")) {
            testConfig(defaults);
        } else
            logger.info("** Success! Login name [{}], apiKey [{}]", defaults.getDefaultUser(), defaults.getApiKey());


    }

    private static void configureLogger(Boolean debug) {
        Logger.getRootLogger().getLoggerRepository().resetConfiguration();
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold((debug ? Level.TRACE : Level.INFO));
        console.activateOptions();

        //add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);

        logger = LoggerFactory.getLogger(Configure.class);
    }

    static File getFile(String configFile, Namespace ns) {
        File file = new File(ns.getString("path") + "/" + configFile);
        File path = new File(ns.getString("path"));
        if (!path.exists() && !path.mkdir()) {
            logger.error("Error making path {}", ns.getString("path"));
            System.exit(-1);
        }
        return file;
    }

    static Namespace getCommandLineArgs(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("configure")
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

        parser.addArgument("-c", "--path")
                .setDefault("./conf")
                .required(false)
                .help("Configuration file path");

        parser.addArgument("-u", "--user")
                .required(false)
                .help("User authorised to create registrations");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        return ns;
    }

    private static boolean configure(File file, ClientConfiguration defaults) {
        String version;

        String engineURL = defaults.getEngineURL();
        //Boolean resetUser = getBooleanValue("** Have you configured FlockData with an alternative security domain?", "N");

        String user = defaults.getDefaultUser();
        String company = defaults.getCompany();
        version = null;

        String userName = null;
        while (version == null) {
            userName = null;
            do {
                String message = "** Enter server URL ";
                String prompt = "Path to FlockData ";
                if (engineURL != null)
                    prompt = prompt + "(" + engineURL + ")";
                engineURL = getValue(prompt + ":", message, engineURL);
            } while (engineURL == null);

            while (userName == null) {
                //logger.error("No admin user supplied to register accounts. Please call [configure -u=user]");
                // System.exit(-2);
                userName = getValue("Admin account (admin): ", "** Login name", "admin");
            }

            internalPass = getPassword(userName);

            version = getVersion(engineURL, userName, internalPass);
        } // While unauthorised
        logger.info("Success in connecting to FlockData");
        internalUser = userName;
        do {
            String message = "** Enter a unique user name to register for data access. ";
            String prompt = "User name to register ";
            if (user != null)
                prompt = prompt + "(" + user + ")";
            user = getValue(prompt + ":", message, user);
        } while (user == null);

        do {
            String message = "** Enter the company name for the data access user [" + user + "]. ";
            String prompt = "Company name to register ";
            if (company != null)
                prompt = prompt + "(" + company + ")";
            company = getValue(prompt + ":", message, company);
        } while (company == null);

        SystemUserResultBean suResult = registerUser(engineURL, internalUser, internalPass, user, company);

        if (suResult != null) {
            version = getVersion(engineURL, suResult.getApiKey());
        } else {
            logger.info("Unable to register the data access user name with the admin account [{}]", internalUser);
            System.exit(-1);
        }
        if (!suResult.getCompanyName().equalsIgnoreCase(company)) {
            logger.error("The Login name [{}] is already in use with another company. Login names must be unique per company. Try using an email address", user);
            System.exit(-1);
        }
        if (version != null) {
            defaults.setDefaultUser(user);
            defaults.setCompany(suResult.getCompanyName());
            defaults.setApiKey(suResult.getApiKey());
        }


        writeConfiguration(file, defaults);
        return true;
    }

    private static String getVersion(String engineURL, String internalUser, String internalPass) {
        String result = pingServerAuth(engineURL, internalUser, internalPass);
        if (result != null && !result.equalsIgnoreCase("pong!"))
            return null;

        return result;
    }

    private static String pingServer(ClientConfiguration defaults, String engineURL) {
        String pingResult;
        if (engineURL != null && !engineURL.equals("") && !engineURL.startsWith("http"))
            engineURL = "http://" + engineURL;

        pingResult = ping(engineURL);

        if (pingResult == null || pingResult.equalsIgnoreCase("err"))
            logger.info("!! Unable to locate FlockData on {}", engineURL);
        else {
            logger.info("** Success in locating FlockData on {}", engineURL);
            defaults.setEngineURL(engineURL);
        }
        return engineURL;
    }

    private static Boolean getBooleanValue(String prompt, String message, String def) {
        String yn = getValue(prompt, message, def);
        if (yn.toLowerCase().startsWith("y"))
            yn = "true";
        else if (yn.toLowerCase().startsWith("n"))
            yn = "false";
        return Boolean.parseBoolean(yn);
    }

    private static void testConfig(ClientConfiguration defaults) {
        String version = getVersion(defaults.getEngineURL(), defaults.getApiKey());
        if (version == null)
            logger.error("!! Error communicating with FlockData using parameters {}", defaults);
        else
            logger.info("** Success communicating with FlockData {} with parameters {}", version, defaults);
    }

    private static void writeConfiguration(File file, ClientConfiguration defaults) {
        try {
            Properties properties = defaults.getAsProperties();
            OutputStream out = new FileOutputStream(file);
            properties.store(out, null);
            logger.debug("** Configuration defaults written to {} ", file.getAbsoluteFile().toString());
        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
    }

    static ClientConfiguration readConfiguration(File file) {
        if (file.exists()) {
            // Load the defaults
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(file));
                return new ClientConfiguration(prop);
            } catch (IOException e) {
                logger.error("Unexpected", e);
            }
            return new ClientConfiguration();
        }

        return new ClientConfiguration();
    }

    private static SystemUserResultBean registerUser(String serverName, String intName, String intPass, String userName, String company) {
        FdRestWriter restClient = new FdRestWriter(serverName, intName, intPass, 0);
        return restClient.registerProfile(intName, intPass, userName, company);
    }

    static String pingServerAuth(String serverName, String userName, String password) {
        FdRestWriter restClient = new FdRestWriter(serverName, userName, password, 0);
        return restClient.pingAuth(userName, password);
    }

    static String ping(String url) {
        FdRestWriter restClient = new FdRestWriter(url, null, null, 0);
        return restClient.ping();
    }

    static String getVersion(String serverName, String apiKey) {
        if (apiKey == null)
            return null;
        FdRestWriter restClient = new FdRestWriter(serverName, apiKey, 0);
        Map<String, Object> result = restClient.health();
        if (result == null || result.get("error") != null)
            return null;

        return result.get("fd-engine.version").toString();
    }

    private static String getPassword(String userName) {
        Console console = System.console();
        if (console != null) {
            // read password into the char array
            char[] pwd = console.readPassword("Password for " + userName + ": ");

            // prints
            return String.copyValueOf(pwd);
        } else {
            try {
                logger.info("Password for [" + userName + "] :");
                return new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (IOException e) {
                logger.error("Unexpected", e);
            }

        }

        return null;
    }

    private static String getValue(String prompt, String message, String defaultValue) {
        Console console = System.console();
        String result = null;
        if (defaultValue != null)
            logger.info(message, defaultValue);
        else
            logger.info(message);

        if (console != null) {
            // read password into the char array

            result = console.readLine(prompt);

        } else {
            try {
                logger.info(prompt);
                result = new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (IOException e) {
                logger.error("Unexpected", e);
            }
        }
        if (result == null || result.equals(""))
            return defaultValue;

        return result;

    }
}
