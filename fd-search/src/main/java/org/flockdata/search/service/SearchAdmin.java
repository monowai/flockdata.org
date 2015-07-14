/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.VersionHelper;
import org.flockdata.track.model.TrackSearchDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
public class SearchAdmin {
    @Autowired
    TrackSearchDao engineDao;

    @Value("${abengine.result}")
    String abEngine;

    @Value("${rabbit.host}")
    String rabbitHost;

    @Value("${rabbit.port}")
    String rabbitPort;

    @Value("${es.mappings}")
    String esMappingPath;
    public String getEsMappingPath(){
        if ( esMappingPath.equals("${es.mappings}"))
            return ""; // Internal
        return esMappingPath;
    }

    public String getEsDefaultSettings() {
        return getEsMappingPath()+ "/fd-default-settings.json";
    }


    String esDefaultMapping= "fd-default-mapping.json";
    public String getEsDefaultMapping(){
        return getEsMappingPath()+"/"+esDefaultMapping;
    }

    private Logger logger = LoggerFactory.getLogger(SearchAdmin.class);

//    @Secured({"ROLE_FD_ADMIN"})
    // DAT-382
    public Map<String, Object> getHealth() {
        String version = VersionHelper.getFdVersion();

        Map<String, Object> healthResults = new HashMap<>();
        healthResults.put("elasticsearch", engineDao.ping());
        healthResults.put("fd-search.version", version);
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        else {
            try {
                // Test for that the supplied path exists
                new FileInputStream(config);
                File file = new File(config);
                // Default to the config path for mappings
                this.esMappingPath = file.getParent();
                logger.info(file.toString());
            } catch (FileNotFoundException e) {
                logger.error("Unexpected error looking for the config file [" + config +"]", e);
            }
        }
        healthResults.put("fd.config", config);
        healthResults.put("es.default settings", getEsDefaultSettings());
        healthResults.put("es.default mapping", getEsDefaultMapping());

        String integration = System.getProperty("fd.integration");
        healthResults.put("fd.integration", integration);
        if ("http".equalsIgnoreCase(integration)) {
            healthResults.put("fdengine.result", abEngine);
        } else {
            healthResults.put("rabbitmq.host", rabbitHost);
            healthResults.put("rabbitmq.port", rabbitPort);
        }
        return healthResults;

    }

    @PostConstruct
    private void doHealth() {
        ObjectMapper om = FlockDataJsonFactory.getObjectMapper();
        try {
            ObjectWriter or = om.writerWithDefaultPrettyPrinter();
            logger.info("\r\n" + or.writeValueAsString(getHealth()));
        } catch (JsonProcessingException e) {

            logger.error("doHealth", e);
        }
    }


}
