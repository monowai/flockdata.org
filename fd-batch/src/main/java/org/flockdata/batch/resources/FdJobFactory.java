package org.flockdata.batch.resources;

import org.flockdata.batch.BatchConfig;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.SQLException;

/**
 * Created by mike on 28/01/16.
 */
@EnableBatchProcessing
@Configuration
@Profile("!dev")
public class FdJobFactory {

    @Autowired
    BatchConfig batchConfig;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("FdBatch");

    @Bean
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean jr = new JobRepositoryFactoryBean();
        jr.setDataSource(repoDataSource());
        jr.setTransactionManager(new ResourcelessTransactionManager());
        jr.setJdbcOperations(batchJdbcTemplate());
        return jr.getObject();
//        JobRepositoryFactoryBean()

    }

    @Bean
    public JdbcTemplate batchJdbcTemplate() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return new JdbcTemplate(repoDataSource());
    }

    @Bean
    JobExplorer jobExplorer() throws Exception {
        JobExplorerFactoryBean result = new JobExplorerFactoryBean();
        result.setJdbcOperations(batchJdbcTemplate());
        return result.getObject();
    }

    @Bean
    BatchConfigurer configurer(@Qualifier("repoDataSource")DataSource dataSource){
        return new DefaultBatchConfigurer(dataSource);
    }

    /**
     * A default job launcher will not use our configured jobRepository, so we're initialising it here
     *
     * @param jobRepository where job metadata is stored
     * @return
     */
    @Bean
    JobLauncher jobLauncher (JobRepository jobRepository) {
        SimpleJobLauncher jl = new SimpleJobLauncher();
        jl.setJobRepository(jobRepository);
        return jl;
    }

    @Bean
    @Qualifier("repoDataSource")
    public DataSource repoDataSource() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.info("Looking for driver class [{}] then will connect on url [{}]", batchConfig.getBatchDriver(), batchConfig.getUrl());

        Class<?> clazz = Class.forName(batchConfig.getBatchDriver());
        Driver driver = (Driver) clazz.newInstance();

        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource(driver, batchConfig.getBatchUrl());

        dataSource.setUsername(batchConfig.getBatchUserName());
        dataSource.setPassword(batchConfig.getBatchPassword());
        DatabasePopulatorUtils.execute(databasePopulator(), dataSource);
        return dataSource;
    }

//    @Value("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
//    private Resource dropScript;

    @Value("classpath:org/springframework/batch/core/schema-hsqldb.sql")
    private Resource schemaScript;

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
        final DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }


    private DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        //populator.addScript(dropScript);
        populator.addScript(schemaScript);
        return populator;
    }
}
