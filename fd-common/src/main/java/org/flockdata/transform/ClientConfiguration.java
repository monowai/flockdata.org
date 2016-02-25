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

package org.flockdata.transform;

import org.flockdata.profile.ContentProfileImpl;

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
public class ClientConfiguration {

    public static final String ENGINE_URL = "fd-engine.api";
    public static final String DEFAULT_USER = "fd-client.defaultUser";
    public static final String API_KEY = "fd-client.apiKey";
    public static final String BATCH_SIZE = "fd-client.batchsize";
    public static final String AMQP = "fd-client.amqp";
    public static final String COMPANY = "fd-client.company";
    public static final String FD_TRACK_EXCHANGE = "fd-track.messaging.exchange";
    public static final String FD_TRACK_QUEUE = "fd-track.messaging.queue";
    public static final String FD_TRACK_BINDING = "fd-track.messaging.binding";
    public static final String RABBIT_HOST = "rabbit.host";
    public static final String RABBIT_USER = "rabbit.user";
    public static final String RABBIT_PASS = "rabbit.pass";
    public static final String RABBIT_PD = "rabbit.persistent";
    private Boolean defConfig = true;
    private String company;
    private boolean async;
    private boolean validateOnly;
    private boolean amqp=true;
    private String trackQueue = "fd.track.queue";
    private String trackExchange = "fd.track.exchange";
    private String trackRoutingKey = "fd.track.binding";
    private String amqpHostAddr = "localhost";
    private String rabbitPass="guest";
    private String rabbitUser="guest";
    private Boolean persistentDelivery= true;
    private int stopRowProcessCount =0;
    private int skipCount=0;
    private File file;
    private boolean reconfigure;
    String engineURL = "http://localhost:8080/api";
    String defaultUser = null;
    String apiKey = null;
    int batchSize = 100;

    public ClientConfiguration() {
        defConfig = true;
    }

    public ClientConfiguration(Properties prop) {
        defConfig = false;
        Object o = prop.get(ENGINE_URL);
        if (o != null)
            setEngineURL(o.toString());
        o = prop.get(DEFAULT_USER);
        if (o != null)
            setDefaultUser(o.toString());

        o = prop.get(API_KEY);
        if (o != null && !o.toString().equals(""))
            setApiKey(o.toString());
        o = prop.get(BATCH_SIZE);
        if (o != null)
            setBatchSize(Integer.parseInt(o.toString()));
        o = prop.get(COMPANY);
        if (o != null)
            setCompany(o.toString());

        o = prop.get(FD_TRACK_EXCHANGE);
        if (o != null)
            setTrackExchange(o.toString());

        o = prop.get(FD_TRACK_QUEUE);
        if (o != null)
            setTrackQueue(o.toString());

        o = prop.get(FD_TRACK_BINDING);
        if (o != null)
            setTrackRoutingKey(o.toString());

        o = prop.get(RABBIT_HOST);
        if (o != null)
            setAmqpHostAddr(o.toString());

        o = prop.get(RABBIT_USER);
        if (o != null)
            setRabbitUser(o.toString());

        o = prop.get(RABBIT_PASS);
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

    @Override
    public String toString() {
        return "ConfigProperties{" +
                "engineURL='" + engineURL + '\'' +
                ", defaultUser='" + defaultUser + '\'' +
                ", amqp='" + amqp + '\'' +
                ", async='" + async + '\'' +
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
        properties.setProperty(ENGINE_URL, engineURL);
        properties.setProperty(DEFAULT_USER, defaultUser);
        properties.setProperty(COMPANY, company);
        properties.setProperty(API_KEY, apiKey);
        properties.setProperty(BATCH_SIZE, Long.toString(batchSize));
        properties.setProperty(FD_TRACK_QUEUE, trackQueue);
        properties.setProperty(FD_TRACK_EXCHANGE, trackExchange);
        properties.setProperty(FD_TRACK_BINDING, trackRoutingKey);
        properties.setProperty(RABBIT_HOST, amqpHostAddr);
        properties.setProperty(RABBIT_USER, rabbitUser);
        properties.setProperty(RABBIT_PASS, rabbitPass);
        properties.setProperty(RABBIT_PD, persistentDelivery.toString());
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

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isAsync() {
        return async;
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

    public String getAmqpHostAddr() {
        return amqpHostAddr;
    }

    public void setAmqpHostAddr(String amqpHostAddr) {
        this.amqpHostAddr = amqpHostAddr;
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
