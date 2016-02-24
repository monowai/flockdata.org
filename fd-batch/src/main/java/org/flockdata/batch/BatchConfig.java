package org.flockdata.batch;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.client.Importer;
import org.flockdata.transform.ClientConfiguration;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties
 *
 * Created by mike on 24/01/16.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    @Value(value = "${fd.client.settings}")
    private String clientSettings;

    @Value(value = "${fd.content.profile}")
    private String contentProfileName;

    @Value(value = "${fd.client.batchsize}")
    private int batchSize;

    @Value(value ="${fd.client.amqp:true}")
    Boolean amqp=true;

    @Value(value ="${fd.query.sql}")
    String sqlQuery;

    @Value("${datasource.url}")
    private String url;
    @Value("${datasource.username:'sa'}")
    private String userName;
    @Value("${datasource.password:''}")
    private String password;
    @Value("${datasource.driver}")
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

    public String getClientSettings() {
        return clientSettings;
    }

    public String getContentProfileName() {
        return contentProfileName;
    }

    public ClientConfiguration getClientConfig(){
        String[] args = {"-amqp="+amqp.toString(), "-b "+batchSize, "-c "+ getClientSettings()};
        try {
            return Importer.getConfiguration(args);
        } catch (ArgumentParserException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public String getSqlQuery() {
        return sqlQuery;
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
}
