/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.integration;

import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;
import org.flockdata.helper.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * FlockData TaskExecutors
 *
 * @author mholdsworth
 * @since 13/07/2015
 */
@Configuration
@EnableAsync(mode = AdviceMode.PROXY)
@Profile("fd-server")
public class ExecutorConfig extends AsyncConfigurerSupport {

  private Logger logger = LoggerFactory.getLogger("configuration");

  @Value("${org.fd.track.executor.poolSize:8-15}")
  private String trackPoolSize;

  @Value("${org.fd.track.executor.queueCapacity:3}")
  private String trackQueueCapacity;

  @Value("${org.fd.tag.executor.poolSize:5-10}")
  private String tagPoolSize;

  @Value("${org.fd.tag.executor.queueCapacity:5}")
  private String tagQueueCapacity;

  @Value("${org.fd.search.executor.poolSize:5-10}")
  private String searchPoolSize;

  @Value("${org.fd.search.executor.queueCapacity:5}")
  private String searchQueueCapacity;

  @Value("${org.fd.store.executor.poolSize:5-10}")
  private String storePoolSize;

  @Value("${org.fd.store.executor.queueCapacity:5}")
  private String storeQueueCapacity;

  @Value("${org.fd.log.executor.poolSize:5-10}")
  private String logPoolSize;

  @Value("${org.fd.log.executor.queueCapacity:5}")
  private String logQueueCapacity;

  @Value("${org.fd.engine.executor.poolSize:5-10}")
  private String enginePoolSize;

  @Value("${org.fd.engine.executor.queueCapacity:5}")
  private String engineQueueCapacity;

  @Bean(name = "fd-track")
  public Executor trackExecutor() {
    return getExecutor("fd-track", trackPoolSize, Integer.parseInt(trackQueueCapacity));
  }

  @Bean(name = "fd-engine")
  public Executor engineExecutor() {
    return getExecutor("fd-engine", enginePoolSize, Integer.parseInt(engineQueueCapacity));
  }

  @Bean(name = "fd-log")
  public Executor logExecutor() {
    return getExecutor("fd-log", logPoolSize, Integer.parseInt(logQueueCapacity));
  }

  @Bean(name = "fd-tag")
  public Executor tagExecutor() {
    return getExecutor("fd-tag", tagPoolSize, Integer.parseInt(tagQueueCapacity));
  }

  @Bean(name = "fd-search")
  public Executor searchExecutor() {
    return getExecutor("fd-search", searchPoolSize, Integer.parseInt(searchQueueCapacity));
  }

  @Bean(name = "fd-store")
  public Executor storeExecutor() {
    return getExecutor("fd-store", storePoolSize, Integer.parseInt(storeQueueCapacity));
  }

  private Executor getExecutor(String name, String poolSize, int qCapacity) {
    return ExecutorHelper.getExecutor(name, poolSize, qCapacity);
  }

  @PostConstruct
  void logStatus() {
    logger.info("**** Threadpool executors have been initialised");
  }

}
