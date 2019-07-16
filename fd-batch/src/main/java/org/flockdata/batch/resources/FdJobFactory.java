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

package org.flockdata.batch.resources;

import java.sql.Driver;
import java.sql.SQLException;
import javax.sql.DataSource;
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

/**
 * @author mholdsworth
 * @since 28/01/2016
 */
@EnableBatchProcessing
@Configuration
@Profile( {"fd-batch", "fd-batch-dev"})
public class FdJobFactory {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("FdBatch");
    @Autowired
    BatchConfig batchConfig;
    @Value("classpath:org/springframework/batch/core/schema-hsqldb.sql")
    private Resource schemaScript;

    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean jr = new JobRepositoryFactoryBean();
        jr.setDataSource(repoDataSource());
        jr.setTransactionManager(new ResourcelessTransactionManager());
        jr.setJdbcOperations(batchJdbcTemplate());
        return jr.getObject();
//        JobRepositoryFactoryBean()

    }

    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
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
    @Profile( {"fd-batch", "fd-batch-dev"})
    BatchConfigurer configurer(@Qualifier("repoDataSource") DataSource dataSource) {
        return new DefaultBatchConfigurer(dataSource);
    }

    /**
     * A default job launcher will not use our configured jobRepository, so we're initialising it here
     *
     * @param jobRepository where job metadata is stored
     * @return
     */
    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
    JobLauncher jobLauncher(JobRepository jobRepository) {
        SimpleJobLauncher jl = new SimpleJobLauncher();
        jl.setJobRepository(jobRepository);
        return jl;
    }

    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
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

    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
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
