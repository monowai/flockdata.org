package org.flockdata.search.integration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync(mode = AdviceMode.PROXY)
@PropertySource(value = "classpath:/application.properties,file:${fd.config},file:${fd.auth.config}", ignoreResourceNotFound = true)
public class AsyncConfig extends AsyncConfigurerSupport {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${fd-search.poolSize?:50}")
    private String searchPoolSize;

    @Value("${fd-search.searchCapacity?:2}")
    private String searchQueueCapacity;



    @Bean(name = "fd-search")
    public ExecutorService searchExecutor() {
        return getExecutor("fd-search", searchPoolSize, getValue(searchQueueCapacity, 2));
    }

    private ExecutorService getExecutor(String name, String poolSize, int  qCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        String vals[] = StringUtils.split(poolSize, "-");

        executor.setCorePoolSize(Integer.parseInt(vals[0]));
        if ( vals.length ==2 )
            executor.setMaxPoolSize(Integer.parseInt(vals[1]));
        else
            executor.setMaxPoolSize(Integer.parseInt(vals[0]));
        executor.setQueueCapacity(qCapacity);
        executor.setThreadNamePrefix(name + "-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }

    private Integer getValue(String input, Integer def){
        if ( input == null || input.equals("@null"))
            return def;
        return Integer.parseInt(input);
    }

    @PostConstruct
    void logStatus() {
        logger.info("**** Async Config Executors Initialised");
    }

}
