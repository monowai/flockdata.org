package com.auditbucket.client;

import com.auditbucket.profile.ImportProfile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 12:38 PM
 */
public class ClientConfiguration {
    private String company;

    public ClientConfiguration(Properties prop) {
        Object o = prop.get("engineURL");
        if ( o!=null)
            setEngineURL(o.toString());
        o = prop.get("defaultUser");
        if ( o!=null)
            setDefaultUser(o.toString());
        o = prop.get("apiKey");
        if ( o!=null)
            setApiKey(o.toString());
        o = prop.get("batchSize");
        if ( o!=null)
            setBatchSize(Integer.parseInt(o.toString()));
        o = prop.get("company");
        if ( o!=null)
            setCompany(o.toString());

    }

    public ClientConfiguration() {
    }

    public String getEngineURL() {
        if ( engineURL!=null && !engineURL.equals("") && !engineURL.startsWith("http"))
            engineURL=  "http://" +engineURL;
        return engineURL;
    }

    public void setEngineURL(String engineURL) {
        this.engineURL = engineURL;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public void setDefaultUser(String defaultUser) {
        this.defaultUser = defaultUser;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    String engineURL = "http://localhost:8080/ab-engine";
    String defaultUser = null;
    String apiKey = "";
    int batchSize = 100;

    @Override
    public String toString() {
        return "ConfigProperties{" +
                "engineURL='" + engineURL + '\'' +
                ", defaultUser='" + defaultUser + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", batchSize=" + batchSize +
                '}';
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Properties getAsProperties() {
        Properties properties = new Properties();
        properties.setProperty("engineURL", engineURL);
        properties.setProperty("defaultUser", defaultUser);
        properties.setProperty("company", company);
        properties.setProperty("apiKey", apiKey);
        properties.setProperty("batchSize", Long.toString(batchSize));
        return properties;
    }

    public static ImportProfile getImportParams(String profile) throws IOException {
        ImportProfile importProfile;
        ObjectMapper om = new ObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, com.auditbucket.profile.ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, com.auditbucket.profile.ImportProfile.class);
            } else
                // Defaults??
                importProfile = new com.auditbucket.profile.ImportProfile();
        }
        //importParams.setWriter(restClient);
        return importProfile;
    }

}
