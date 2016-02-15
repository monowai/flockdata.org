package org.flockdata.search.integration;

import org.flockdata.helper.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync(mode = AdviceMode.PROXY)
@PropertySource(value = "classpath:/application.properties,file:${fd.config},file:${fd.auth.config}", ignoreResourceNotFound = true)
public class AsyncConfig extends AsyncConfigurerSupport {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${fd-search.poolSize}")
    private String searchPoolSize;

    @Value("${fd-search.searchCapacity:2}")
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
