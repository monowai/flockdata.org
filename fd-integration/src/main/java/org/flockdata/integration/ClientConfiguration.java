/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * Configures fd-client from properties source from a configuration file
 *
 * @author mholdsworth
 * @since 30/04/2014
 * @tag Configuration, FdClient
 */
@Configuration
public class ClientConfiguration {

    public static final String AMQP = "org.fd.client.amqp";
    public static final String KEY_MSG_KEY = "fd-apiKey";
    public static final String KEY_MSG_TYPE = "fd-type";
    private static final String KEY_ENGINE_API = "org.fd.engine.api";
    private static final String KEY_COMPANY = "org.fd.client.default.company";
    private static final String KEY_FORTRESS = "org.fd.client.default.fortress";
    private static final String KEY_API_KEY = "org.fd.client.apikey";
    private static final String KEY_HTTP_USER = "org.fd.client.http.user";
    private static final String KEY_HTTP_PASS = "org.fd.client.http.pass";
    private static final String KEY_BATCH_SIZE = "org.fd.client.batchsize";
    private static final String KEY_TRACK_QUEUE = "org.fd.track.messaging.queue";
    private static final String KEY_TRACK_EXCHANGE = "org.fd.messaging.exchange";
    private static final String KEY_TRACK_BINDING = "org.fd.track.messaging.binding";
    @Value ("${"+ KEY_COMPANY +":flockdata}")
    private String company;

    @Value ("${"+ KEY_FORTRESS +":#{null}}")
    private String fortress;

    @Value("${"+ KEY_TRACK_QUEUE +":fd.track.queue}")
    private String trackQueue = "fd.track.queue";

    @Value("${"+ KEY_TRACK_EXCHANGE +":fd}")
    private String fdExchange = "fd";

    @Value("${"+ KEY_TRACK_BINDING +":fd.track.queue}")
    private String trackRoutingKey = "fd.track.queue}";

    @Value("${"+ KEY_ENGINE_API +":http://localhost:8080}")
    private
    String engineUrl = "http://localhost:8080";

    @Value("${"+ KEY_API_KEY +":}")
    private
    String apiKey = null;

    @Value("${"+ KEY_HTTP_USER +":mike}")
    private
    String httpUser = null;

    @Value("${"+ KEY_HTTP_PASS +":123}")
    private
    String httpPass = null;

    @Value("${"+ KEY_BATCH_SIZE +":1}")
    private
    int batchSize = 1;

    private boolean validateOnly;
    private boolean amqp=true;

    private int stopRowProcessCount =0;
    private int skipCount=0;
    private File file;
    private Collection<String> filesToImport = new ArrayList<>();

    public ClientConfiguration() {
//        defConfig = true;
    }

    @Deprecated // use injection
    public ClientConfiguration(Properties prop) {
        //defConfig = false;
        Object o = prop.get(KEY_ENGINE_API);
        if (o != null)
            setServiceUrl(o.toString());
        o = prop.get(KEY_API_KEY);
        if (o != null && !o.toString().equals(""))
            setApiKey(o.toString());
        o = prop.get(KEY_BATCH_SIZE);
        if (o != null)
            setBatchSize(Integer.parseInt(o.toString()));
        o = prop.get(KEY_COMPANY);
        if (o != null)
            setCompany(o.toString());

        o = prop.get(KEY_TRACK_EXCHANGE);
        if (o != null)
            setFdExchange(o.toString());

        o = prop.get(KEY_TRACK_QUEUE);
        if (o != null)
            setTrackQueue(o.toString());

        o = prop.get(KEY_TRACK_BINDING);
        if (o != null)
            setTrackRoutingKey(o.toString());


    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getServiceUrl() {
        if ( engineUrl !=null && !engineUrl.equals("") && !engineUrl.startsWith("http"))
            engineUrl =  "http://" + engineUrl;
        return engineUrl;
    }

    public ClientConfiguration setServiceUrl(String engineUrl) {
        this.engineUrl = engineUrl;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public ClientConfiguration setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    @PostConstruct
    public String toString() {
        return "ConfigProperties{" +
                ""+ KEY_ENGINE_API +"='" + engineUrl + '\'' +
                ", "+ KEY_API_KEY +"='" + ( !(apiKey!=null && apiKey.equals("")) ?"** set **": "!! not set !!")+ '\'' +
                ", "+ KEY_BATCH_SIZE +"=" + batchSize +
                '}';
    }

    public String getCompany() {
        return company;
    }

    private void setCompany(String company) {
        this.company = company;
    }

    /**
     *
     * @return optionally defined default fortress
     */
    public String getFortress() {
        return fortress;
    }

    public boolean isAsync() {
        return false;
    }

    public boolean isAmqp() {
        return amqp;
    }

    public String getTrackQueue() {
        return trackQueue;
    }

    private void setTrackQueue(String trackQueue) {
        this.trackQueue = trackQueue;
    }

    public String getFdExchange() {
        return fdExchange;
    }

    private void setFdExchange(String fdExchange) {
        this.fdExchange = fdExchange;
    }

    public String getTrackRoutingKey() {
        return trackRoutingKey;
    }

    private void setTrackRoutingKey(String trackRoutingKey) {
        this.trackRoutingKey = trackRoutingKey;
    }

    /**
     * stop count
     * @return stop processing after this row is reached
     */
    public int getStopRowProcessCount() {
        return stopRowProcessCount;
    }

    public void setStopRowProcessCount(int stopRowProcessCount) {
        this.stopRowProcessCount = stopRowProcessCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Autowired
    void loadConfigs(@Value("${fd.client.import:}") final String str) throws Exception {
        if (str != null && !str.equals("")) {
            filesToImport = Arrays.asList(str.split(";"));

        }
    }

    public Collection<String> getFilesToImport() {
        return filesToImport;
    }

    public String getHttpUser() {
        return httpUser;
    }

    public ClientConfiguration setHttpUser(String httpUser) {
        this.httpUser = httpUser;
        return this;
    }

    public String getHttpPass() {
        return httpPass;
    }

    public ClientConfiguration setHttpPass(String httpPass) {
        this.httpPass = httpPass;
        return this;
    }

    public boolean isApiKeyValid() {
        return !(apiKey == null || apiKey.isEmpty());
    }
}
