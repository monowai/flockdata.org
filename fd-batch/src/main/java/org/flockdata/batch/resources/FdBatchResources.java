package org.flockdata.batch.resources;

import org.flockdata.batch.BatchConfig;
import org.flockdata.batch.listener.FlockDataJobListener;
import org.flockdata.batch.listener.FlockDataSkipListener;
import org.flockdata.batch.listener.FlockDataStepListener;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.SQLException;

/**
 * Encapsulates basic DataSource functionality
 * Created by mike on 28/01/16.
 */
@Configuration
public class FdBatchResources {

    @Autowired
    BatchConfig batchConfig;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("FdBatch");

    @Bean
    public JdbcTemplate jdbcTemplate() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return new JdbcTemplate(dataSource());
    }

    /**
     * <beans:bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
     * <beans:property name="driverClassName" value="${datasource.driver}"/>
     * <beans:property name="url" value="${datasource.url}"/>
     * <beans:property name="username" value="${datasource.username}"/>
     * <beans:property name="password" value="${datasource.password}"/>
     * </beans:bean>
     *
     * @return
     * @throws SQLException
     */
    @Bean()
    @Profile("!dev")
    @Qualifier("dataSource")
    @Primary
    public DataSource dataSource() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.info("Looking for driver class [{}] then will connect on url [{}]", batchConfig.getDriver(), batchConfig.getUrl());
        Class<?> clazz = Class.forName(batchConfig.getDriver());
        Driver driver = (Driver) clazz.newInstance();

        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource(driver, batchConfig.getUrl());

        dataSource.setUsername(batchConfig.getUserName());
        dataSource.setPassword(batchConfig.getPassword());
        return dataSource;
    }

    @Bean
    public JobExecutionListener jobListener() {
        return new FlockDataJobListener();
    }

    @Bean
    public StepExecutionListener stepListener() {
        return new FlockDataStepListener();
    }

    @Bean
    public SkipListener skipListener() {
        return new FlockDataSkipListener();
    }


}
