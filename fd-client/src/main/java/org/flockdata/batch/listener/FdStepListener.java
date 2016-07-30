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

package org.flockdata.batch.listener;

import org.flockdata.transform.PayloadBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

public class FdStepListener implements StepExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(FdStepListener.class);

    @Autowired
    PayloadBatcher batchLoader;

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
        batchLoader.flush();
        return stepExecution.getExitStatus();
    }


}
