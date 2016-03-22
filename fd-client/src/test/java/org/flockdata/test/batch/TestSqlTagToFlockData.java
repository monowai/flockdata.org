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

package org.flockdata.test.batch;


import junit.framework.TestCase;
import org.flockdata.batch.BatchConfig;
import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.batch.resources.FdEntityProcessor;
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.batch.resources.FdTagProcessor;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.shared.FdBatcher;
import org.flockdata.test.client.MockFdWriter;
import org.flockdata.transform.PayloadBatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"dev", "fd-batch"})
@SpringApplicationConfiguration({ BatchConfig.class,
                                  FdBatchResources.class,
                                  ClientConfiguration.class,
                                  MockFdWriter.class,
                                  FdTagProcessor.class,
                                  FdEntityProcessor.class,
                                  FdRowMapper.class,
                                  FdBatcher.class,
                                  HsqlDataSource.class,
                                  JobLauncherTestUtils.class,
                                  SqlTagJob.class
                                })

@TestPropertySource({"/fd-batch.properties","/application_dev.properties"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TestSqlTagToFlockData extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    ClientConfiguration clientConfiguration;

    @Autowired
    PayloadBatcher payloadBatcher;

    @Test
    @Sql({"/batch/sql/schema.sql", "/batch/sql/data.sql", "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
    public void testDummy() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        assertTrue(clientConfiguration.getBatchSize()>1);
        // This check works because 2 is < the configured batch size
        TestCase.assertEquals("Number of rows loaded ex data.sql does not match", 2, payloadBatcher.getTags().size());
        for (TagInputBean tagInputBean : payloadBatcher.getTags()) {
            assertEquals("Person", tagInputBean.getLabel());
            assertNotNull(tagInputBean.getCode());
        }
    }

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }


}
