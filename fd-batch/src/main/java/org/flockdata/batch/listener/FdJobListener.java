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

package org.flockdata.batch.listener;

import org.flockdata.integration.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile( {"fd-batch", "fd-batch-dev"})
public class FdJobListener implements JobExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(FdJobListener.class);

  @Autowired
  Template batchLoader;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    logger.info("Startup of batch {}", jobExecution.getJobInstance().getJobName());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    logger.info("Exit code: {}", jobExecution.getExitStatus().getExitCode());
    logger.info("End of batch {}", jobExecution.getJobInstance().getJobName());
    batchLoader.flush();
  }

}
