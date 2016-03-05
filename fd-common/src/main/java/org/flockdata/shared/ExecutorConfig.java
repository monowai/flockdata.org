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

package org.flockdata.shared;

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

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;

/**
 * FlockData TaskExecutors
 *
 * Created by mike on 13/07/15.
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
        return getExecutor( "fd-engine", enginePoolSize, Integer.parseInt(engineQueueCapacity)  );
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

    private Executor getExecutor(String name, String poolSize, int  qCapacity) {
       return ExecutorHelper.getExecutor(name, poolSize, qCapacity);
    }

    @PostConstruct
    void logStatus() {
        logger.info("**** Executors (ex.shared) have been initialised");
    }

}
