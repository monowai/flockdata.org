/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.VersionHelper;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.configure.SearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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


    public Map<String, Object> getHealth() {
        String version = VersionHelper.getFdVersion();

        Map<String, Object> healthResults = new HashMap<>();
        healthResults.put("elasticsearch", engineDao.ping());
        healthResults.put("fd.search.version", version);
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        else {
            // Test for that the supplied path exists
            // Default to the config path for mappings
            logger.info(searchConfig.getEsMappingPath());
        }
        healthResults.put("fd.config", config);
        healthResults.put("fd.search.es.settings", searchConfig.getEsDefaultSettings());
        healthResults.put("fd.search.es.mapping", searchConfig.getEsMappingPath());

        return healthResults;

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
