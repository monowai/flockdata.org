/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.store.configuration;

import org.flockdata.helper.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class ExecutorConfig extends AsyncConfigurerSupport {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${org.fd.store.executor.poolSize}")
    private String storePoolSize;

    @Value("${org.fd.store.executor.queueCapacity}")
    private String storeQueueCapacity;


    @Override
    public Executor getAsyncExecutor() {
        return storeExecutor();   // Default executor
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
        logger.info("**** Executors for async support have been initialised");
    }

}
