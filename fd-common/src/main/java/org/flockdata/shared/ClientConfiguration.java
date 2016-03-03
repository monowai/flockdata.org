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

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.transform.ProfileReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
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

    public static final String KEY_ENGINE_API = "org.fd.engine.api";
    public static final String KEY_LOGIN_USER = "org.fd.client.login.user";
    public static final String KEY_COMPANY = "org.fd.client.default.company";
    public static final String KEY_API_KEY = "org.fd.client.apikey";
    public static final String KEY_BATCH_SIZE = "org.fd.client.batchsize";
    public static final String AMQP = "org.fd.client.amqp";

    public static final String KEY_TRACK_QUEUE = "org.fd.track.messaging.queue";
    public static final String KEY_TRACK_EXCHANGE = "org.fd.track.messaging.exchange";
    public static final String KEY_TRACK_BINDING = "org.fd.track.messaging.binding";
    public static final String KEY_RABBIT_HOST = "rabbit.host";
    public static final String KEY_RABBIT_USER = "rabbit.user";
    public static final String KEY_RABBIT_PASS = "rabbit.pass";
    public static final String KEY_RABBIT_PD = "rabbit.persistent";
    public static final String KEY_MSG_KEY = "fd-apiKey";
    private Boolean defConfig = true;

    @Value ("${"+ KEY_COMPANY +":flockdata}")
    private String company;

    @Value("${"+ KEY_TRACK_QUEUE +":fd.track.queue}")
    private String trackQueue = "fd.track.queue";

    @Value("${"+ KEY_TRACK_EXCHANGE +":fd.track.exchange}")
    private String trackExchange = "fd.track.exchange";

    @Value("${"+ KEY_TRACK_BINDING +":fd.track.binding}")
    private String trackRoutingKey = "fd.track.binding";

    @Value("${"+ KEY_RABBIT_HOST +":localhost}")
    private String rabbitHost = "localhost";

    @Value("${"+ KEY_RABBIT_PASS +":guest}")
    private String rabbitPass="guest";

    @Value("${"+ KEY_RABBIT_USER +":guest}")
    private String rabbitUser="guest";

    @Value("${"+ KEY_ENGINE_API +":http://localhost:8080/api}")
    String engineURL = "http://localhost:8080/api";

    // An admin user connecting to the API to retrieve a an APIKey
    @Value("${"+ KEY_LOGIN_USER +":}")
    String loginUser = null;

    @Value("${"+ KEY_API_KEY +":}")
    String apiKey = null;

    @Value("${"+ KEY_BATCH_SIZE +":1}")
    int batchSize = 1;

    private Boolean persistentDelivery= true;
    private boolean validateOnly;
    private boolean amqp=true;

    private int stopRowProcessCount =0;
    private int skipCount=0;
    private File file;
    private boolean reconfigure;


    public ClientConfiguration() {
        defConfig = true;
    }

    public ClientConfiguration(Properties prop) {
        defConfig = false;
        Object o = prop.get(KEY_ENGINE_API);
        if (o != null)
            setEngineURL(o.toString());
        o = prop.get(KEY_LOGIN_USER);
        if (o != null)
            setLoginUser(o.toString());

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

        o = prop.get(KEY_RABBIT_HOST);
        if (o != null)
            setRabbitHost(o.toString());

        o = prop.get(KEY_RABBIT_USER);
        if (o != null)
            setRabbitUser(o.toString());

        o = prop.get(KEY_RABBIT_PASS);
        if (o != null)
            setRabbitPass(o.toString());

    }

    public String getEngineURL() {
        if ( engineURL!=null && !engineURL.equals("") && !engineURL.startsWith("http"))
            engineURL=  "http://" +engineURL;
        return engineURL;
    }

    public void setEngineURL(String engineURL) {
        this.engineURL = engineURL;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
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
                ""+ KEY_ENGINE_API +"='" + engineURL + '\'' +
                ", "+ KEY_RABBIT_HOST +"='" + rabbitHost+ '\'' +
                ", "+ KEY_RABBIT_USER +"='" + rabbitUser+ '\'' +
                ", "+ KEY_API_KEY +"='" + ( !(apiKey!=null && apiKey.equals("")) ?"** set **": "!! not set !!")+ '\'' +
                ", "+ KEY_BATCH_SIZE +"=" + batchSize +
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
        properties.setProperty(KEY_ENGINE_API, engineURL);
        properties.setProperty(KEY_LOGIN_USER, loginUser);
        properties.setProperty(KEY_COMPANY, company);
        properties.setProperty(KEY_API_KEY, apiKey);
        properties.setProperty(KEY_BATCH_SIZE, Long.toString(batchSize));
        properties.setProperty(KEY_TRACK_QUEUE, trackQueue);
        properties.setProperty(KEY_TRACK_EXCHANGE, trackExchange);
        properties.setProperty(KEY_TRACK_BINDING, trackRoutingKey);
        properties.setProperty(KEY_RABBIT_HOST, rabbitHost);
        properties.setProperty(KEY_RABBIT_USER, rabbitUser);
        properties.setProperty(KEY_RABBIT_PASS, rabbitPass);
        properties.setProperty(KEY_RABBIT_PD, persistentDelivery.toString());
        return properties;
    }

    /**
     *
     * @param profile Fully file name
     * @return initialized ImportProfile
     * @throws IOException
     * @throws ClassNotFoundException
     * @deprecated - use org.flockdata.transform.ProfileConfiguration
     */
    public static ContentProfileImpl getImportProfile(String profile) throws IOException, ClassNotFoundException {
        return ProfileReader.getImportProfile(profile);
    }

    public boolean isAsync() {
        return false;
    }

    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public boolean isAmqp() {
        return amqp;
    }

    public void setAmqp(boolean amqp) {
        setAmqp(amqp, true);
    }

    public void setAmqp(boolean amqp, boolean persistentDelivery) {
        this.amqp = amqp;
        setPersistentDelivery(persistentDelivery);
    }

    public String getTrackQueue() {
        return trackQueue;
    }

    public void setTrackQueue(String trackQueue) {
        this.trackQueue = trackQueue;
    }

    public String getTrackExchange() {
        return trackExchange;
    }

    public void setTrackExchange(String trackExchange) {
        this.trackExchange = trackExchange;
    }

    public String getTrackRoutingKey() {
        return trackRoutingKey;
    }

    public void setTrackRoutingKey(String trackRoutingKey) {
        this.trackRoutingKey = trackRoutingKey;
    }

    public String getRabbitHost() {
        return rabbitHost;
    }

    public void setRabbitHost(String rabbitHost) {
        this.rabbitHost = rabbitHost;
    }

    public void setRabbitPass(String rabbitPass) {
        this.rabbitPass = rabbitPass;
    }

    public String getRabbitPass() {
        return rabbitPass;
    }

    public void setRabbitUser(String rabbitUser) {
        this.rabbitUser = rabbitUser;
    }

    public String getRabbitUser() {
        return rabbitUser;
    }

    public boolean getPersistentDelivery() {
        return persistentDelivery;
    }

    public boolean isPersistentDelivery() {
        return persistentDelivery;
    }

    public void setPersistentDelivery(boolean persistentDelivery) {
        this.persistentDelivery = persistentDelivery;
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

    public void setReconfigure(boolean reconfigure) {
        this.reconfigure = reconfigure;
    }

    public boolean isReconfigure() {
        return reconfigure;
    }

    /**
     * Supports unit testing
     * @return true if this is intialised simply as a default configuration
     */
    public Boolean isDefConfig() {
        return defConfig;
    }

}
