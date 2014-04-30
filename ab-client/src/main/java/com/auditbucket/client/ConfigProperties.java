package com.auditbucket.client;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 12:38 PM
 */
public class ConfigProperties {
    private String company;

    public String getEngineURL() {
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

    public int getDefaultBatch() {
        return defaultBatch;
    }

    public void setDefaultBatch(int defaultBatch) {
        this.defaultBatch = defaultBatch;
    }

    String engineURL = "http://localhost:8080/ab-engine";
    String defaultUser = null;
    String apiKey = "";
    int defaultBatch = 100;

    @Override
    public String toString() {
        return "ConfigProperties{" +
                "engineURL='" + engineURL + '\'' +
                ", defaultUser='" + defaultUser + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", defaultBatch=" + defaultBatch +
                '}';
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
