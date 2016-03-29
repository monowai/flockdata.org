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

package org.flockdata.engine.configure;

import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * FD-Cache configuration settings
 * Created by mike on 23/03/16.
 */
@Configuration
@EnableCaching
@Profile("fd-server")
public class CacheConfiguration {
    private Logger logger = LoggerFactory.getLogger("configuration");

    @Bean
    public CacheManager cacheManager() {

        GuavaCache tagCache = new GuavaCache("tag", CacheBuilder.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build());

        GuavaCache geoCache = new GuavaCache("geoData", CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .build());

        GuavaCache fortressUser = new GuavaCache("fortressUser", CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build());

        GuavaCache sysUserApi = new GuavaCache("sysUserApiKey", CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build());

        GuavaCache company = new GuavaCache("company", CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build());

        GuavaCache docType = new GuavaCache("documentType", CacheBuilder.newBuilder()
                .maximumSize(50)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build());

        GuavaCache labels = new GuavaCache("labels", CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(120, TimeUnit.MINUTES)
                .build());

        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();

        simpleCacheManager.setCaches(Arrays.asList(tagCache,
                docType,
                geoCache,
                fortressUser,
                labels,
                company,
                sysUserApi));

        logger.info("**** Configured for Guava caching");
        return simpleCacheManager;
    }
}
