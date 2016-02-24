package org.flockdata.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

@Component
public class FlockDataSkipListener implements SkipListener<Object, Object> {
    private static final Logger logger = LoggerFactory.getLogger(FlockDataSkipListener.class);

    private StepExecution stepExecution;

    @Override
    public void onSkipInRead(Throwable t) {
        logger.error("Row skipped in the reading phase", t);
        stepExecution.setTerminateOnly();
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        logger.error("Row skipped in the writing phase", t);
        stepExecution.setTerminateOnly();
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        logger.error("Row skipped in the processing phase", t);
        stepExecution.setTerminateOnly();
    }

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

}
