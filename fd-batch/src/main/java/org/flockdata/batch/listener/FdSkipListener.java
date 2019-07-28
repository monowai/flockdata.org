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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @tag Batch
 */
@Component
@Profile("fd-batch")
public class FdSkipListener implements SkipListener<Object, Object> {
  private static final Logger logger = LoggerFactory.getLogger(FdSkipListener.class);

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
