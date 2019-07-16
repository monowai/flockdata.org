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

package org.flockdata.batch.resources;

import java.sql.Driver;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flockdata.batch.BatchConfig;
import org.flockdata.batch.listener.FdJobListener;
import org.flockdata.batch.listener.FdSkipListener;
import org.flockdata.batch.listener.FdStepListener;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.stereotype.Component;

/**
 * Encapsulates basic DataSource functionality
 * Kept here for reference only
 *
 * @author mholdsworth
 * @tag Batch, FdClient
 * @since 28/01/2016
 */
@Component
@Profile( {"fd-batch", "fd-batch-dev"})
public class FdBatchResources {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("FdBatch");
    private final BatchConfig batchConfig;

    @Autowired
    public FdBatchResources(BatchConfig batchConfig) {
        this.batchConfig = batchConfig;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return new JdbcTemplate(dataSource());
    }

    /**
     * {@literal
     * <beans:bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
     * <beans:property name="driverClassName" value="${datasource.driver}"/>
     * <beans:property name="url" value="${datasource.url}"/>
     * <beans:property name="username" value="${datasource.username}"/>
     * <beans:property name="password" value="${datasource.password}"/>
     * </beans:bean>
     * }
     *
     * @return datasource to read from
     * @throws SQLException           connection errors
     * @throws ClassNotFoundException driver couldn't be found
     * @throws IllegalAccessException driver issue
     * @throws InstantiationException driver issue
     */
    @Bean
    @Profile( {"fd-batch", "fd-batch-dev"})
    @Qualifier("dataSource")
    @Primary
    public DataSource dataSource() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (!batchConfig.getDriver().equals("")) {
            logger.info("Looking for driver class [{}] then will connect on url [{}]", batchConfig.getDriver(), batchConfig.getUrl());
            Class<?> clazz = Class.forName(batchConfig.getDriver());
            Driver driver = (Driver) clazz.newInstance();

            final SimpleDriverDataSource dataSource = new SimpleDriverDataSource(driver, batchConfig.getUrl());

            dataSource.setUsername(batchConfig.getUserName());
            dataSource.setPassword(batchConfig.getPassword());
            return dataSource;
        }
        return null;
    }

    @Bean
    public JobExecutionListener fdJobListener() {
        return new FdJobListener();
    }

    @Bean
    public StepExecutionListener fdStepListener() {
        return new FdStepListener();
    }

    @Bean
    public SkipListener fdSkipListener() {
        return new FdSkipListener();
    }


}
