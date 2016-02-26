/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
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
        logger.info("**** Async Config Executors Initialised");
    }

}
