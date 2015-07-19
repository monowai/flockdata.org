/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.configure;

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
import java.util.concurrent.Executor;

/**
 * FlockData TaskExecutors
 *
 * Created by mike on 13/07/15.
 */
@Configuration
@EnableAsync(mode = AdviceMode.PROXY)
@PropertySource(value = "classpath:/config.properties,file:${fd.config},file:${fd.auth.config}", ignoreResourceNotFound = true)
public class AsyncConfig extends AsyncConfigurerSupport {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${fd-track.poolSize}")
    private String trackPoolSize;

    @Value("${fd-track.queueCapacity}")
    private String trackQueueCapacity;

    @Value("${fd-tag.poolSize}")
    private String tagPoolSize;

    @Value("${fd-tag.queueCapacity}")
    private String tagQueueCapacity;

    @Value("${fd-search.poolSize}")
    private String searchPoolSize;

    @Value("${fd-search.searchCapacity:@null}")
    private String searchQueueCapacity;

    @Value("${fd-store.poolSize}")
    private String storePoolSize;

    @Value("${fd-store.queueCapacity}")
    private String storeQueueCapacity;

    @Value("${fd-log.poolSize}")
    private String logPoolSize;

    @Value("${fd-log.queueCapacity}")
    private String logQueueCapacity;

    @Value("${fd-engine.poolSize}")
    private String enginePoolSize;

    @Value("${fd-engine.queueCapacity}")
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
        return getExecutor("fd-search", searchPoolSize, getValue(searchQueueCapacity, 2));
    }

    @Bean(name = "fd-store")
    public Executor storeExecutor() {
        return getExecutor("fd-store", storePoolSize, Integer.parseInt(storeQueueCapacity));
    }

    private Executor getExecutor(String name, String poolSize, int  qCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        String vals[] = StringUtils.split(poolSize, "-");

        executor.setCorePoolSize(Integer.parseInt(vals[0]));
        if ( vals.length ==2 )
            executor.setMaxPoolSize(Integer.parseInt(vals[1]));
        else
            executor.setMaxPoolSize(Integer.parseInt(vals[0]));
        executor.setQueueCapacity(qCapacity);
        executor.setThreadNamePrefix(name +"-");
        executor.initialize();
        return executor;
    }

    private Integer getValue(String input, Integer def){
        if ( input == null || input.equals("@null"))
            return def;
        return Integer.parseInt(input);
    }

    @PostConstruct
    void logStatus() {
        logger.info("**** Async Config Initialised");
    }

}
