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
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.batch.resources.FdTagProcessor;
import org.flockdata.batch.resources.FdTagWriter;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Template;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.unit.client.FdMockIo;
import org.flockdata.test.unit.client.FdTemplateMock;
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

@RunWith(SpringRunner.class)
@ActiveProfiles( {"dev", "fd-batch-dev"})
@ContextConfiguration(classes = {BatchConfig.class,
    FdBatchResources.class,
    ClientConfiguration.class,
    FdMockIo.class,
    FdTemplateMock.class,
    FdTagProcessor.class,
    FdTagWriter.class,
    FdEntityProcessor.class,
    FdRowMapper.class,
    HsqlDataSource.class,
    JobLauncherTestUtils.class,
    SqlTagStep.class
})

@TestPropertySource( {"/fd-batch.properties", "/application_dev.properties"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TestSqlTagToFlockData extends AbstractTransactionalJUnit4SpringContextTests {

  @Autowired
  private ClientConfiguration clientConfiguration;
  @Autowired
  private Template fdTemplate;
  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Test
  @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"/batch/sql/countries.sql", "/batch/sql/country-data.sql", "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
  @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = {"classpath:org/springframework/batch/core/schema-drop-hsqldb.sql"})
  public void testDummy() throws Exception {
    JobExecution jobExecution = jobLauncherTestUtils.launchJob();
    assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
    assertTrue(clientConfiguration.batchSize() > 1);
    // This check works because 2 is < the configured batch size
    TestCase.assertEquals("Number of rows loaded ex entity-data.sql does not match", 2, fdTemplate.getTags().size());
    for (TagInputBean tagInputBean : fdTemplate.getTags()) {
      assertEquals("Country", tagInputBean.getLabel());
      assertNotNull(tagInputBean.getName());
      assertEquals(3, tagInputBean.getCode().length());
      assertNotNull(tagInputBean.getCode());
    }
  }


  @Bean
  public JobLauncherTestUtils getJobLauncherTestUtils() {
    return new JobLauncherTestUtils();
  }


}
