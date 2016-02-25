package org.flockdata.batch;


import junit.framework.TestCase;
import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.batch.resources.FdWriter;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
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
                                  FdMockWriter.class,
                                  HsqlDataSource.class,
                                  JobLauncherTestUtils.class,
                                  SqlQueryJob.class
                                })
public class TestSqlToFlockData extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    FdWriter fdWriter;


    @Test
    @Sql({"/sql/schema.sql", "/sql/data.sql", "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
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
