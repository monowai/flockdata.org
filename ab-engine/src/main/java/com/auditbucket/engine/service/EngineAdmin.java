/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.dao.AuditDao;
import com.auditbucket.helper.VersionHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
@Transactional
public class EngineAdmin {

    @Autowired
    AuditDao auditDAO;

    private String abSearch;

    private String rabbitHost;

    private String rabbitPort;

    private Logger logger = LoggerFactory.getLogger(EngineAdmin.class);

    @Value("${rabbit.host:@null}")
    protected void setRabbitHost(String rabbitHost) {
        if ("@null".equals(rabbitHost)) this.rabbitHost = null;
        else this.rabbitHost = rabbitHost;

    }

    @Value("${rabbit.port:@null}")
    protected void setRabbitPort(String rabbitPort) {
        if ("@null".equals(rabbitPort)) this.rabbitPort = null;
        else this.rabbitPort = rabbitPort;

    }

    @Value("${absearch.make:@null}")
    protected void setAbSearch(String absearchMake) {
        if ("@null".equals(absearchMake)) this.abSearch = null;
        else this.abSearch = absearchMake;

    }

    public Map<String, String> getHealth() {
        String version = VersionHelper.getABVersion();
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("ab-engine.version", version);
        healthResults.put("ab-engine", auditDAO.ping());
        String config = System.getProperty("ab.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("ab.integration");
        healthResults.put("ab.integration", integration);
        if ("http".equalsIgnoreCase(integration)) {
            healthResults.put("absearch.make", abSearch);
        } else {
            healthResults.put("rabbitmq.host", rabbitHost);
            healthResults.put("rabbitmq.port", rabbitPort);
        }
        return healthResults;

    }


    private void doHealth() {
        ObjectMapper om = new ObjectMapper();
        try {
            ObjectWriter or = om.writerWithDefaultPrettyPrinter();
            logger.info("\r\n" + or.writeValueAsString(getHealth()));

        } catch (JsonProcessingException e) {

            logger.error("doHealth", e);
        }
    }

}
