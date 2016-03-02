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
import org.flockdata.batch.resources.FdWriter;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("dev")
@SpringApplicationConfiguration({ BatchConfig.class,
                                  FdBatchResources.class,
        ClientConfiguration.class,
                                  FdMockWriter.class,
                                  HsqlDataSource.class,
                                  JobLauncherTestUtils.class,
                                  SqlQueryJob.class
                                })
@TestPropertySource("/fd-batch.properties")
public class TestSqlToFlockData extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    FdWriter fdWriter;


    @Test
    @Sql({"/batch/sql/schema.sql", "/batch/sql/data.sql", "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
    public void testDummy() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        // This check works because 2 is < the configured batch size
        TestCase.assertEquals("Number of rows loaded ex data.sql does not match", 2, fdWriter.getFdLoader().getEntities().size());
        for (EntityInputBean entityInputBean : fdWriter.getFdLoader().getEntities()) {
            assertNotNull(entityInputBean.getContent());
            assertNotNull("Primary Key was not set via the content profile", entityInputBean.getCode());
            assertNotNull(entityInputBean.getContent().getData().get("ID"));
            assertNotNull(entityInputBean.getContent().getData().get("FIRSTNAME"));
        }
    }

    @Autowired
    FdBatchResources batchResources;

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }


}
