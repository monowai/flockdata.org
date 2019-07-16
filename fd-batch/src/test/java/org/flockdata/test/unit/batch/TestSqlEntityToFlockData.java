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

package org.flockdata.test.unit.batch;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import junit.framework.TestCase;
import org.flockdata.batch.BatchConfig;
import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.batch.resources.FdEntityProcessor;
import org.flockdata.batch.resources.FdEntityWriter;
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.batch.resources.FdTagProcessor;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Template;
import org.flockdata.test.unit.client.FdMockIo;
import org.flockdata.test.unit.client.FdTemplateMock;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * FdClient SpringBatch test runner
 *
 * @tag Batch, FdClient, Test
 */
@RunWith(SpringRunner.class)
@ActiveProfiles( {"dev", "fd-batch-dev"})
@ContextConfiguration(classes = {BatchConfig.class,
    FdBatchResources.class,
    ClientConfiguration.class,
    FdMockIo.class,
    FdTemplateMock.class,
    FdTagProcessor.class,
    FdEntityProcessor.class,
    FdEntityWriter.class,
    FdRowMapper.class,
    HsqlDataSource.class,
    JobLauncherTestUtils.class,
    SqlEntityStep.class
})
@TestPropertySource( {"/fd-batch.properties", "/application_dev.properties"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TestSqlEntityToFlockData extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private Template fdTemplate;

    @Test
    @Sql( {"/batch/sql/entity.sql", "/batch/sql/entity-data.sql", "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = {"classpath:org/springframework/batch/core/schema-drop-hsqldb.sql"})
    public void testDummy() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        assertTrue(clientConfiguration.getBatchSize() > 1);
        // This check works because 2 is < the configured batch size
        TestCase.assertEquals("Number of rows loaded ex entity-data.sql does not match", 2, fdTemplate.getEntities().size());
        for (EntityInputBean entityInputBean : fdTemplate.getEntities()) {
            assertNotNull(entityInputBean.getContent());
            assertNotNull("Primary Key was not set via the content profile", entityInputBean.getCode());
            assertNotNull(entityInputBean.getContent().getData().get("ID"));
            assertNotNull(entityInputBean.getContent().getData().get("FIRSTNAME"));
        }
    }

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

}
