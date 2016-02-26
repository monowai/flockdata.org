/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.client;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.transform.ClientConfiguration;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 12:42 PM
 */
public class Configure {
    protected static String configFile = "client.config";
    private static String internalUser = null;
    private static String internalPass = null;

    public static void main(String args[]) throws ArgumentParserException {

        ClientConfiguration defaults = readConfigProfile(args);

        internalUser = defaults.getLoginUser();

        if (!defaults.getFile().exists() || defaults.isReconfigure()) {

            System.out.println(String.format("** %sconfiguration process", (defaults.isReconfigure() ? "re" : "initial ")));
            String engineURL = defaults.getEngineURL();
            System.out.println(String.format("** Looking for FlockData on [%s]", engineURL));
            pingServer(defaults, engineURL);
            defaults.setApiKey(null);

            String version = getVersion(engineURL, defaults.getApiKey());

            if (version != null) {
                System.out.println(String.format("** Success!! All configured default settings are working! Talking to FlockData version %s", version));
                if (defaults.isReconfigure()) {
                    Boolean yes = getBooleanValue("Reconfigure? ", "Do you still wish to reconfigure your settings? (n)", "n");
                    if (yes) {
                        configure(defaults.getFile(), defaults);
                    }
                } else {
                    writeConfiguration(defaults.getFile(), defaults);
                }

            } else {
                configure(defaults.getFile(), defaults);
            }
        }
        // Test the configuration with the defaults provided
        if (getNameSpace().getBoolean("test")) {
            testConfig(defaults);
        } else
            System.out.println(String.format("** Success! Login name [%s], apiKey [%s]", defaults.getLoginUser(), defaults.getApiKey()));


    }

    public static ClientConfiguration readConfigProfile(String[] args) throws ArgumentParserException {
        nameSpace = InitializationSupport.getConfigureNamespace(args);
        //logger = InitializationSupport.configureLogger(getNameSpace().getBoolean("debug"));
        internalUser = getNameSpace().getString("user");

        File file = getFile(getNameSpace());
        ClientConfiguration clientConfig = getConfiguration(file);
        clientConfig.setFile(file);

        Object o = getNameSpace().getBoolean("reconfig");
        if ( o!=null )
            clientConfig.setReconfigure(Boolean.parseBoolean(o.toString()));

        return clientConfig;
    }

    static File getFile(Namespace ns) {
        String fullPath = ns.getString("config");
        if ( fullPath != null) {
            fullPath = fullPath.trim();
            return makeConfigPath(fullPath);
        }
        String path = ns.getString("cpath").trim();
        return makeConfigPath(path+"/"+configFile);
    }

    private static File makeConfigPath(String path) {
        File fullPath = new File(path);
        if ( fullPath.getParent() !=null ) {
            String parent = fullPath.getParent();
            File fPath = new File(parent);
            if (!fPath.exists() && !fPath.mkdir()) {
                System.out.println("Error making path [" + path + "] Working dir [" + System.getProperty("user.dir") + "]");
                System.exit(-1);
            }
        }
        return fullPath;
    }

    private static boolean configure(File file, ClientConfiguration defaults) {
        String version;

        String engineURL = defaults.getEngineURL();
        //Boolean resetUser = getBooleanValue("** Have you configured FlockData with an alternative security domain?", "N");

        String user = defaults.getLoginUser();
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
        System.out.println("Success in connecting to FlockData");
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
            // Test that we can connect with the generated API Key
            getVersion(engineURL, suResult.getApiKey());
        } else {
            System.out.println(String.format("Unable to register the data access user name with the admin account [%s]", internalUser));
            System.exit(-1);
        }
        if (!suResult.getCompanyName().equalsIgnoreCase(company)) {
            System.out.println(String.format("The Login name [%s] is already in use with another company. Login names must be unique per company. Try using an email address", user));
            System.exit(-1);
        }
        defaults.setEngineURL(engineURL);
        defaults.setLoginUser(user);
        defaults.setCompany(suResult.getCompanyName());
        defaults.setApiKey(suResult.getApiKey());
        writeConfiguration(file, defaults);

        return true;
    }

    private static String getVersion(String engineURL, String internalUser, String internalPass) {
        String result = pingServerAuth(engineURL, internalUser, internalPass);
        if (result != null && !result.startsWith("pong"))
            return null;

        return result;
    }

    private static String pingServer(ClientConfiguration defaults, String engineURL) {
        String pingResult;
        if (engineURL != null && !engineURL.equals("") && !engineURL.startsWith("http"))
            engineURL = "http://" + engineURL;

        pingResult = ping(engineURL);

        if (pingResult == null || pingResult.equalsIgnoreCase("err"))
            System.out.println(String.format("!! Unable to locate FlockData on %s", engineURL));
        else {
            System.out.println(String.format("** Success in locating FlockData on %s", engineURL));
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
            System.out.println(String.format("!! Error communicating with FlockData using parameters {%s}", defaults));
        else
            System.out.println(String.format("** Success communicating with FlockData %s with parameters %s", version, defaults));
    }

    private static void writeConfiguration(File file, ClientConfiguration defaults) {
        try {
            Properties properties = defaults.getAsProperties();
            OutputStream out = new FileOutputStream(file);
            properties.store(out, null);
            System.out.println(String.format("** Configuration defaults written to %s ", file.getAbsoluteFile().toString()));
        } catch (IOException e) {
            System.out.println("Unexpected: " + e.getMessage());
        }
    }

    @Deprecated // Autowire ClientConfiguration or do new ClientConfiguration()
    public static ClientConfiguration getConfiguration(File file) {
        System.out.println("Reading client configuration from " + file.getAbsoluteFile());
        if (file.exists()) {
            // Load the defaults
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(file));
                return new ClientConfiguration(prop);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Returning a default client configuration");
            }
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

        return result.get("flockdata.version").toString();
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
                System.out.println(String.format("Password for [%s] :", userName));
                return new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (IOException e) {
                System.out.println("Unexpected: " + e.getMessage());
            }

        }

        return null;
    }

    private static String getValue(String prompt, String message, String defaultValue) {
        Console console = System.console();
        String result = null;
        if (defaultValue != null)
            System.out.println(String.format(message, defaultValue));
        else
            System.out.println(message);

        if (console != null) {
            // read password into the char array

            result = console.readLine(prompt);

        } else {
            try {
                System.out.println(prompt);
                result = new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (IOException e) {
                //logger.error("Unexpected", e);
                System.exit(1);
            }
        }
        if (result == null || result.equals(""))
            return defaultValue;

        return result;

    }

    @Deprecated // Get your own namespace
    public static Namespace getNameSpace() {
        return nameSpace;
    }

    private static Namespace nameSpace = null;



}
