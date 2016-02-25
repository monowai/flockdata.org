package org.flockdata.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.client.Importer;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.transform.ClientConfiguration;
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
 * Configuration properties
 * <p>
 * Created by mike on 24/01/16.
 */
@Configuration
@PropertySources({
        @PropertySource(value = "classpath:/fd-batch.properties"),
        @PropertySource(value = "file:${fd.batch.properties}", ignoreResourceNotFound = true)
})
public class BatchConfig {
    private Logger logger = LoggerFactory.getLogger(BatchConfig.class);
    @Value("${fd-client.settings}")
    private String clientSettings;

    private int batchSize;

    @Value("${fd-client.batchsize}")
    void setBatchSize(String batch){
        this.batchSize = Integer.parseInt(batch);
    }

    @Value("${fd-client.amqp}")
    String amqp = "true";

    @Value("${source.datasource.url}")
    private String url;
    @Value("${source.datasource.username:'sa'}")
    private String userName;
    @Value("${source.datasource.password:''}")
    private String password;
    @Value("${source.datasource.driver}")
    private String driver;

    @Value("${batch.datasource.url}")
    private String batchUrl;
    @Value("${batch.datasource.username:'sa'}")
    private String batchUserName;
    @Value("${batch.datasource.password:''}")
    private String batchPassword;
    @Value("${batch.datasource.driver}")
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

    String getClientSettings() {
        return clientSettings;
    }

    ClientConfiguration getClientConfig() {
        String[] args = { "-c " + getClientSettings()};
        try {

            ClientConfiguration clientConfiguration =  Importer.getConfiguration(args);
            clientConfiguration.setBatchSize(batchSize);
            clientConfiguration.setAmqp(Boolean.parseBoolean(amqp));
            return clientConfiguration;
        } catch (ArgumentParserException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
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
    void loadConfigs(@Value("${fd-client.configs:}") final String str)  throws Exception {
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
