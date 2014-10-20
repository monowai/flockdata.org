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

import org.flockdata.profile.ImportProfile;
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

    String engineURL = "http://localhost:8080/fd-engine";
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

    public static ImportProfile getImportParams(String profile) throws IOException, ClassNotFoundException {
        ImportProfile importProfile;
        ObjectMapper om = new ObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else
                // Defaults??
                importProfile = new ImportProfile();
        }
        //importParams.setWriter(restClient);
        return importProfile;
    }

}