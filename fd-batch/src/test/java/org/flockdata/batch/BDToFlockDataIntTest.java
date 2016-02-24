package org.flockdata.batch;


import junit.framework.TestCase;
import org.flockdata.batch.resources.FdBatchLoader;
import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = { Processor.class,   // Loads config
                                  BatchConfig.class, // Provides access to configured properties
                                  FdBatchResources.class, // Datasource
                                  JobLauncherTestUtils.class // Executor
         })
@ActiveProfiles("dev")
public class BDToFlockDataIntTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    FdBatchLoader fdBatchLoader;

    @Sql ( {"/sql/schema.sql", "/sql/data.sql"} )

    @Test
    public void testDummy() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        // This check works because 2 is < the configured batch size
        TestCase.assertEquals("Number of rows loaded ex data.sql does not match", 2, fdBatchLoader.getFdLoader().getEntities().size());
        for (EntityInputBean entityInputBean : fdBatchLoader.getFdLoader().getEntities()) {
            assertNotNull(entityInputBean.getContent());
            assertNotNull ("Primary Key was not set via the content profile", entityInputBean.getCode());
            assertNotNull(entityInputBean.getContent().getData().get("ID"));
            assertNotNull(entityInputBean.getContent().getData().get("FIRSTNAME"));
        }
    }

    @Autowired
    FdBatchResources batchResources ;

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils (){
        return new JobLauncherTestUtils();
    }


}
