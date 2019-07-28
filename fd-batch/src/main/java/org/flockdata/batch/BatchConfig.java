/*
 *  Copyright 2012-2017 the original author or authors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Service;

/**
 * FlockData spring-batch configuration class
 * <p>
 * creates a configured instance with an FdTemplate to communicate with FlockData
 * Loads org.fd.client.configs from your fd-batch.properties file that will in-turn read a
 * YAML file for mapping between an SQL query and a ContentProfile
 *
 * @author mholdsworth
 * @tag Batch, Integration, Configuration, FdClient
 * @see org.flockdata.integration.ClientConfiguration
 * @see FdIoInterface
 * @see org.flockdata.integration.Template
 * @since 24/01/2016
 */
@PropertySources( {
    @PropertySource(value = "classpath:/fd-batch.properties"),
    @PropertySource(value = "file:${org.fd.batch.properties}", ignoreResourceNotFound = true)
})
@Profile( {"fd-batch", "fd-batch-dev"})
@Service
public class BatchConfig {
  private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private final FdIoInterface fdIoInterface;
  @Value("${org.fd.client.amqp:true}")
  Boolean amqp = true;
  private Logger logger = LoggerFactory.getLogger(BatchConfig.class);
  @Value("${org.fd.client.batchsize:1}")
  private int batchSize;
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
  private Map<String, StepConfig> config = new HashMap<>();

  @Autowired
  public BatchConfig(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  /**
   * Initialisation of the configuration that will be handled by this class
   * ToDo: These need to support refreshing in non-command line environments
   *
   * @param str comma separated list of configuration files to initialise
   * @throws Exception anything goes wrong
   */
  @Autowired
  void loadConfigs(@Value("${org.fd.client.configs:}") final String str) throws Exception {
    if (str != null && !str.equals("")) {
      List<String> configs = Arrays.asList(str.split(","));

      for (String config : configs) {
        try {
          StepConfig stepConfig = loadStepConfig(config);
          logger.info("Loaded configuration {}", config);
          this.config.put(stepConfig.getStep(), stepConfig);
        } catch (Exception e) {
          logger.error(e.getMessage() + " processing " + config);
          throw e;
        }

      }
    }
    logger.info("Data source {}", getUrl());
  }

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

  private StepConfig loadStepConfig(String stepName) throws IOException, ClassNotFoundException {
    StepConfig stepConfig;
    stepConfig = readConfig(stepName.trim());

    return stepConfig;
  }

  public StepConfig getStepConfig(String stepName) {
    StepConfig stepConfig = config.get(stepName);
    if (stepConfig == null) {
      logger.error("The requested step configuration [{}] does not exist - known step configs [{}]", stepName, Arrays.toString(config.values().toArray()));
      throw new IllegalArgumentException("The requested step configuration " + stepName + " does not exist. Known configs are [" + Arrays.toString(config.values().toArray()) + "]");
    }
    try {
      if (stepConfig.getContentModel() == null) {
        stepConfig.setContentModel(getModelForStep(stepConfig));
      }
    } catch (IOException e) {
      logger.error("Failed to resolve content model for " + stepName);
      throw new RuntimeException(e);
    }
    return stepConfig;
  }

  private StepConfig readConfig(String fileName) throws IOException {
    StepConfig stepConfig;
    InputStream file = null;
    try {
      file = getClass().getClassLoader().getResourceAsStream(fileName);
      if (file == null)
      // running from JUnit can only read this as a file input stream
      {
        file = new FileInputStream(fileName);
      }
      stepConfig = loadStepConfig(file);
    } catch (IOException e) {
      logger.info("Unable to read {} as a file. Error {} \r\n, trying as a URL...", fileName, e.getMessage());
      stepConfig = loadStepConfig(new URL(fileName));
    } finally {
      if (file != null) {
        file.close();
      }
    }
    return stepConfig;

  }

  private ContentModel getModelForStep(StepConfig stepConfig) throws IOException {
    ContentModel contentModel = null;
    if (stepConfig.getModel() != null) {
      // Resolve from local file system
      contentModel = ContentModelDeserializer.getContentModel(stepConfig.getModel());
      if (contentModel == null)
      // Check the server
      {
        contentModel = fdIoInterface.getContentModel(stepConfig.getModel());
      }
      stepConfig.setContentModel(contentModel);
    } else if (stepConfig.getModelKey() != null) {
      contentModel = fdIoInterface.getContentModel(stepConfig.getModelKey());
    }
    return contentModel;
  }

  private StepConfig loadStepConfig(InputStream file) throws IOException {
    return mapper.readValue(file, StepConfig.class);
  }

  private StepConfig loadStepConfig(URL url) throws IOException {
    return mapper.readValue(url, StepConfig.class);

  }

}
