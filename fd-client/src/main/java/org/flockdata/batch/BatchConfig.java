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

package org.flockdata.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.transform.ProfileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlockData spring-batch configuration class
 *
 * creates a configured instance of an FdLoader to communicate with FlockData
 * Loads org.fd.client.configs from your fd-batch.properties file that will in-turn read a
 * YAML file for mapping between an SQL query and a ContentProfile
 *
 * <p>
 * Created by mike on 24/01/16.
 */
@Configuration
@PropertySources({
        @PropertySource(value = "classpath:/fd-batch.properties"),
        @PropertySource(value = "file:${org.fd.batch.properties}", ignoreResourceNotFound = true)
})
public class BatchConfig {
    private Logger logger = LoggerFactory.getLogger(BatchConfig.class);

    private int batchSize;

    @Value("${org.fd.client.batchsize:1}")
    void setBatchSize(String batch){
        this.batchSize = Integer.parseInt(batch);
    }

    @Value("${org.fd.client.amqp:true}")
    Boolean amqp = true;

    @Value("${source.datasource.url}")
    private String url;
    @Value("${source.datasource.username}")
    private String userName;
    @Value("${source.datasource.password}")
    private String password;
    @Value("${source.datasource.driver}")
    private String driver;

    @Value("${batch.datasource.url:jdbc:hsqldb:mem:sb}")
    private String batchUrl;
    @Value("${batch.datasource.username:'sa'}")
    private String batchUserName;
    @Value("${batch.datasource.password: }")
    private String batchPassword;
    @Value("${batch.datasource.driver:org.hsqldb.jdbc.JDBCDriver}")
    private String batchDriver;



    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getBatchUrl() {
        return batchUrl;
    }

    public String getBatchUserName() {
        return batchUserName;
    }

    public String getBatchPassword() {
        return batchPassword;
    }

    public String getBatchDriver() {
        return batchDriver;
    }

    Map<String, StepConfig> config = new HashMap<>();

    @Autowired
    void loadConfigs(@Value("${org.fd.client.configs:}") final String str)  throws Exception {
        if (str != null && !str.equals("")) {
            List<String> configs = Arrays.asList(str.split(","));

            for (String config : configs) {
                try {
                    StepConfig stepConfig = loadStepConfig(config);
                    logger.info("Loaded configuration {}", config);
                    this.config.put(stepConfig.getStep(), stepConfig);
                } catch (Exception e ){
                    logger.error (e.getMessage() +" processing " + config);
                    throw e;
                }

            }
        }
    }

    public StepConfig getStepConfig(String stepName){
        return config.get(stepName);
    }

    private StepConfig loadStepConfig(String stepName) throws IOException, ClassNotFoundException {
        StepConfig stepConfig = getStepConfig(stepName);
        if (stepConfig == null) {
            stepConfig = readConfig(stepName.trim());
            if (stepConfig.getProfile() != null) {
                ContentProfile contentProfile = ProfileReader.getImportProfile(stepConfig.getProfile());
                stepConfig.setContentProfile(contentProfile);
            }

        }
        return stepConfig;
    }

    private StepConfig readConfig(String fileName) throws IOException {
        StepConfig stepConfig = null;
        InputStream file = null;
        try {
            file = getClass().getClassLoader().getResourceAsStream(fileName);
            if (file == null)
                // running from JUnit can only read this as a file input stream
                file = new FileInputStream(fileName);
            stepConfig = loadStepConfig(file);
        } catch (IOException e) {
            logger.info("Unable to read {} as a file, trying as a URL...", fileName);
            stepConfig = loadStepConfig(new URL(fileName));
        } finally {
            if (file != null) {
                file.close();
            }
        }
        return stepConfig;

    }

    private StepConfig loadStepConfig(URL url) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(url, StepConfig.class);

    }

    private StepConfig loadStepConfig(InputStream file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(file, StepConfig.class);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
