package org.flockdata.batch.listener;

import org.flockdata.batch.resources.FdWriter;
import org.flockdata.helper.FlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

public class FlockDataStepListener implements StepExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(FlockDataStepListener.class);

    @Autowired
    FdWriter batchLoader;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("====== Step {} ======", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("");
        logger.info("READING");
        logger.info("- Number of Read: " + stepExecution.getReadCount());
        logger.info("- Number of ReadSkip: " + stepExecution.getReadSkipCount());
        logger.info("VALIDATION");
        logger.info("- Number of ProcessSkip: " + stepExecution.getProcessSkipCount());
        logger.info("WRITING");
        logger.info("- Number of Write: " + stepExecution.getWriteCount());
        logger.info("- Number of WriteSkip: " + stepExecution.getWriteSkipCount());
        logger.info("TOTAL (read+validation+write)");
        logger.info("- Total Number of Skip: " + stepExecution.getSkipCount());
        logger.info("- Number of Commit (including technical steps): " + stepExecution.getCommitCount());
        logger.info("- Number of Rollback (including technical steps): " + stepExecution.getRollbackCount());
        logger.info("");
        try {
            batchLoader.flush();
        } catch (FlockException e) {
            e.printStackTrace();
        }
        return stepExecution.getExitStatus();
    }


}
