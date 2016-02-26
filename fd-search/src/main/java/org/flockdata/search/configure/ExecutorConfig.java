/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.search.configure;

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

@Configuration
@EnableAsync(mode = AdviceMode.PROXY)
public class ExecutorConfig extends AsyncConfigurerSupport {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${org.fd.search.executor.poolSize}")
    private String searchPoolSize;

    @Value("${org.fd.search.executor.capacity:2}")
    private String searchQueueCapacity;

    @Override
    public Executor getAsyncExecutor() {
        return fdSearchExecutor();   // Default executor
    }

    @Bean(name = "fd-search")
    public Executor fdSearchExecutor() {
        return ExecutorHelper.getExecutor("fd-search", searchPoolSize, getValue(searchQueueCapacity, 2));
    }


    private Integer getValue(String input, Integer def){
        if ( input == null || input.equals("@null"))
            return def;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    @PostConstruct
    void logStatus() {
        logger.info("**** Async Config Executors Initialised");
    }

}
