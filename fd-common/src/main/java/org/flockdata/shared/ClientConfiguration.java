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

package org.flockdata.shared;

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
 * User: mike
 * Date: 30/04/14
 * Time: 12:38 PM
 */
@Configuration
public class ClientConfiguration {

    private static final String KEY_ENGINE_API = "org.fd.engine.api";
    private static final String KEY_COMPANY = "org.fd.client.default.company";
    private static final String KEY_FORTRESS = "org.fd.client.default.fortress";
    private static final String KEY_API_KEY = "org.fd.client.apikey";
    private static final String KEY_HTTP_USER = "org.fd.client.http.user";
    private static final String KEY_HTTP_PASS = "org.fd.client.http.pass";
    private static final String KEY_BATCH_SIZE = "org.fd.client.batchsize";
    public static final String AMQP = "org.fd.client.amqp";

    private static final String KEY_TRACK_QUEUE = "org.fd.track.messaging.queue";
    private static final String KEY_TRACK_EXCHANGE = "org.fd.track.messaging.exchange";
    private static final String KEY_TRACK_BINDING = "org.fd.track.messaging.binding";
    public static final String KEY_MSG_KEY = "fd-apiKey";
    public static final String KEY_MSG_TYPE = "fd-type";


    @Value ("${"+ KEY_COMPANY +":flockdata}")
    private String company;

    @Value ("${"+ KEY_FORTRESS +":#{null}}")
    private String fortress;

    @Value("${"+ KEY_TRACK_QUEUE +":fd.track.queue}")
    private String trackQueue = "fd.track.queue";

    @Value("${"+ KEY_TRACK_EXCHANGE +":fd.track.exchange}")
    private String trackExchange = "fd.track.exchange";

    @Value("${"+ KEY_TRACK_BINDING +":fd.track.binding}")
    private String trackRoutingKey = "fd.track.binding";

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
            setTrackExchange(o.toString());

        o = prop.get(KEY_TRACK_QUEUE);
        if (o != null)
            setTrackQueue(o.toString());

        o = prop.get(KEY_TRACK_BINDING);
        if (o != null)
            setTrackRoutingKey(o.toString());


    }

    public String getServiceUrl() {
        if ( engineUrl !=null && !engineUrl.equals("") && !engineUrl.startsWith("http"))
            engineUrl =  "http://" + engineUrl;
        return engineUrl;
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

    /**
     *
     * @return optionally defined default fortress
     */
    public String getFortress() {
        return fortress;
    }

    private void setCompany(String company) {
        this.company = company;
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

    public String getTrackExchange() {
        return trackExchange;
    }

    private void setTrackExchange(String trackExchange) {
        this.trackExchange = trackExchange;
    }

    public String getTrackRoutingKey() {
        return trackRoutingKey;
    }

    private void setTrackRoutingKey(String trackRoutingKey) {
        this.trackRoutingKey = trackRoutingKey;
    }

    public void setStopRowProcessCount(int stopRowProcessCount) {
        this.stopRowProcessCount = stopRowProcessCount;
    }

    /**
     * stop count
     * @return stop processing after this row is reached
     */
    public int getStopRowProcessCount() {
        return stopRowProcessCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    private Collection<String> filesToImport = new ArrayList<>();

    @Autowired
    void loadConfigs(@Value("${fd.client.import:}") final String str) throws Exception {
        if (str != null && !str.equals("")) {
            filesToImport = Arrays.asList(str.split(";"));

        }
    }

    public Collection<String> getFilesToImport() {
        return filesToImport;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getHttpUser() {
        return httpUser;
    }

    public String getHttpPass() {
        return httpPass;
    }

    public ClientConfiguration setServiceUrl(String engineUrl) {
        this.engineUrl = engineUrl;
        return this;
    }

    public ClientConfiguration setHttpUser(String httpUser) {
        this.httpUser = httpUser;
        return this;
    }

    public ClientConfiguration setHttpPass(String httpPass) {
        this.httpPass = httpPass;
        return this;
    }
}
