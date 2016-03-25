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

import org.flockdata.batch.BatchConfig;
import org.flockdata.batch.listener.FlockDataJobListener;
import org.flockdata.batch.listener.FlockDataSkipListener;
import org.flockdata.batch.listener.FlockDataStepListener;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.PayloadBatcher;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.SQLException;

/**
 * Encapsulates basic DataSource functionality
 * Kept here for reference only
 * Created by mike on 28/01/16.
 */
@Configuration
@Component
@Profile("fd-batch")
public class FdBatchResources {

    @Autowired
    BatchConfig batchConfig;

    @Autowired
    PayloadBatcher payloadBatcher;

    @Autowired
    FdEntityProcessor fdEntityProcessor;

    @Autowired
    ItemWriter<EntityInputBean> fdEntityWriter;

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
    @Bean
    @Profile("!dev")
    @Qualifier("dataSource")
    @Primary
    public DataSource dataSource() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if ( !batchConfig.getDriver().equals("")) {
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

    @Bean
    public ItemWriter<EntityInputBean> fdEntityWriter() {
        return new FdEntityWriter(payloadBatcher);
    }

    @Bean
    public ItemWriter<TagInputBean> fdTagWriter() {
        return new FdTagWriter(payloadBatcher);
    }


}
