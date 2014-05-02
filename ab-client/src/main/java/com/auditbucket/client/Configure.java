package com.auditbucket.client;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
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
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Configure.class);
    static String configFile = "client.config";
    static String internalUser = null;
    static String internalPass = null;

    public static void main(String args[]) {


        Namespace ns = getCommandLineArgs(args);
        internalPass = ns.getString("pass");
        internalUser = ns.getString("user");

        File file = getFile(configFile, ns);
        ConfigProperties defaults = readConfiguration(file);

        boolean reconfigure = ns.getBoolean("reconfig");

        if (!file.exists() || reconfigure) {
            if ( internalUser == null ){
                logger.error("No admin user supplied to register accounts. Please call configure -u=user -p=pass");

                System.exit (-2);
            }
            logger.info("** Starting default configuration process");
            String engineURL = defaults.getEngineURL();
            logger.info("** Looking for server with all system defaults");
            defaults.setApiKey(null);

            String version = getVersion(engineURL, defaults.getApiKey());

            if (version != null) {
                logger.info("** Success!! All configured default settings are working! Talking to AbEngine version {}", version);
                if (reconfigure) {
                    Boolean yes = getBooleanValue("Do you still wish to reconfigure your settings? (n)", "n");
                    if (yes) {
                        configure(file, defaults, engineURL);
                    }
                } else {
                    writeConfiguration(file, defaults);
                }

            } else {
                configure(file, defaults, engineURL);
            }
        }
        // Test the configuration with the defaults provided
        if (ns.getBoolean("test")) {
            testConfig(defaults);
        } else
            logger.info("{}", defaults);


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
                .description("Configures the client for connectivity to AuditBucket");

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

        parser.addArgument("-c", "--path")
                .setDefault("./conf")
                .required(false)
                .help("Configuration file path");

        parser.addArgument("-u", "--user")
                .required(false)
                .help("User authorised to create registrations");

        parser.addArgument("-p", "--pass")
                .required(false)
                .help("Password for --user");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        return ns;
    }

    private static boolean configure(File file, ConfigProperties defaults, String engineURL) {
        String pingResult;
        String version;

        pingResult = pingServer(engineURL, null, null);


        while (!pingResult.equalsIgnoreCase("pong!") && !pingResult.equalsIgnoreCase("auth")) {
            logger.error("** Unable to ping AbEngine on [{}]", engineURL);
            engineURL = getValue("** Enter AbEngine URL ({})", engineURL);
            if (engineURL != null) {
                pingResult = pingServer(engineURL, null, null);
            }
            defaults.setEngineURL(engineURL);
        }
        logger.info("** Success in connecting to AuditBucket on {}", engineURL);
        //Boolean resetUser = getBooleanValue("** Have you configured AuditBucket with an alternative security domain?", "N");
        String apiKey = defaults.getApiKey();

        version = getVersion(engineURL, apiKey);
        String user = defaults.getDefaultUser();
        String company = defaults.getCompany();
        while (version == null) {

            do {
                String message = "** Enter a unique login name to register ";
                if ( user!=null )
                    message = message + "default = ({})";
                user = getValue(message, user);
            } while (user == null );
            do{
                String message="** Enter the company name for the login ["+user+"] ";
                if ( company != null )
                    message = message + "default = ({})";
                company = getValue(message, company);
            } while (company == null );

            SystemUserResultBean suResult = registerUser(engineURL, internalUser, internalPass, user, company);

            if (suResult != null) {
                version = getVersion(engineURL, suResult.getApiKey());
            } else {
                logger.info("Unable to register the system user with the login [{}]", internalUser);
                System.exit(-1);
            }
            if ( !suResult.getCompanyName().equalsIgnoreCase(company)){
                logger.error ("The Login name [{}] is already in use with another company. Login names must be unique. Try using an email address", user);
                System.exit(-1);
            }
            if (version != null) {
                defaults.setDefaultUser(user);
                defaults.setCompany(suResult.getCompanyName());
                defaults.setApiKey(suResult.getApiKey());
            }
        }

        writeConfiguration(file, defaults);
        logger.info("** Success!! Configured settings are working.");
        return true;
    }

    private static Boolean getBooleanValue(String message, String def) {
        String yn = getValue(message, def);
        if (yn.toLowerCase().startsWith("y"))
            yn = "true";
        else if (yn.toLowerCase().startsWith("n"))
            yn = "false";
        return Boolean.parseBoolean(yn);
    }

    private static void testConfig(ConfigProperties defaults) {
        String version = getVersion(defaults.getEngineURL(), defaults.getApiKey());
        if (version == null)
            logger.error("** Error communicating with AuditBucket using parameters {}", defaults);
        else
            logger.info("** Success communicating with AuditBucket {} with parameters {}", version, defaults);
    }

    private static void writeConfiguration(File file, ConfigProperties defaults) {
        try {
            Properties properties = defaults.getAsProperties();
            OutputStream out = new FileOutputStream(file);
            properties.store(out, null);
            //ObjectMapper om = new ObjectMapper();

            //om.writerWithDefaultPrettyPrinter().writeValue(file, defaults);
            logger.info("** Configuration defaults written to {} ", file.getAbsoluteFile().toString());
        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
    }

    static ConfigProperties readConfiguration(File file) {
        if (file.exists()) {
            // Load the defaults
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(file));
                return new ConfigProperties(prop);
            } catch (IOException e) {
                logger.error("Unexpected", e);
            }
            return new ConfigProperties();
        }

        return new ConfigProperties();
    }

    private static SystemUserResultBean registerUser(String serverName, String intName, String intPass, String userName, String company) {
        AbRestClient restClient = new AbRestClient(serverName, intName, intPass, 0);
        return restClient.registerProfile(intName, intPass, userName, company);
    }

    static String pingServer(String serverName, String userName, String password) {
        AbRestClient restClient = new AbRestClient(serverName, userName, password, 0);
        return restClient.ping();
    }

    static String getVersion(String serverName, String apiKey) {
        AbRestClient restClient = new AbRestClient(serverName, apiKey, 0);
        Map<String, Object> result = restClient.health();
        if (result == null || result.get("error") != null)
            return null;

        return result.get("ab-engine.version").toString();
    }

    private static String getValue(String message, String defaultValue) {
        if (defaultValue != null)
            logger.info(message, defaultValue);
        else
            logger.info(message);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String value = null;
        try {
            value = br.readLine();
            if (value != null)
                if (value.equals("") || value.equals(defaultValue))
                    return defaultValue;
        } catch (IOException ioe) {
            System.out.println("IO error trying to read your name!");
            System.exit(1);
        }
        return value;
    }
}
