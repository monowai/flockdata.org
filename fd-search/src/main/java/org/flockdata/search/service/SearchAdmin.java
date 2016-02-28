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

package org.flockdata.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.configure.SearchConfig;
import org.flockdata.shared.VersionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
@Configuration
public class SearchAdmin {
    @Autowired
    EntityChangeWriter engineDao;

    @Autowired
    SearchConfig searchConfig;

    private Logger logger = LoggerFactory.getLogger(SearchAdmin.class);

    @Autowired
    VersionHelper versionHelper;
    public Map<String, Object> getHealth() {
        String version = versionHelper.getFdVersion();

        Map<String, Object> healthResults = new HashMap<>();
        healthResults.put("elasticsearch", engineDao.ping());
        healthResults.put("fd.search.version", version);
            // Test for that the supplied path exists
            // Default to the config path for mappings
        healthResults.put("fd.search.es.settings", searchConfig.getEsDefaultSettings());
        healthResults.put("fd.search.es.mapping", searchConfig.getEsMappingPath());

        return healthResults;

    }

    @Bean
    public InfoEndpoint infoEndpoint() {
        return new InfoEndpoint(getHealth());
    }


    @PostConstruct
    void logStatus(){
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
        try {
            ObjectWriter or = om.writerWithDefaultPrettyPrinter();
            logger.info("\r\n" + or.writeValueAsString(getHealth()));
        } catch (JsonProcessingException e) {

            logger.error("doHealth", e);
        }
    }
}
