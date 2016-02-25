package org.flockdata.batch.listener;

import org.flockdata.batch.resources.FdWriter;
import org.flockdata.helper.FlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

public class FlockDataJobListener implements JobExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(FlockDataJobListener.class);

    @Autowired
    FdWriter batchLoader;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("");
        logger.info("Startup of batch {}", jobExecution.getJobInstance().getJobName());
        logger.info("");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Exit code: {}", jobExecution.getExitStatus().getExitCode());
        logger.info("End of batch {}", jobExecution.getJobInstance().getJobName());
        try {
            batchLoader.flush();
        } catch (FlockException e) {
            logger.error ( "Flushing the batch caused an exception");
        }
    }

}
