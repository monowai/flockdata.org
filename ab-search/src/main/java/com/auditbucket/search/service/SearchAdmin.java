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

package com.auditbucket.search.service;

import com.auditbucket.audit.model.AuditSearchDao;
import com.auditbucket.helper.VersionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
public class SearchAdmin {
    @Autowired
    AuditSearchDao auditSearch;

    @Value("${abengine.result}")
    String abEngine;
    private Map<String, Object> health;

    @Value("${rabbit.host}")
    String rabbitHost;

    @Value("${rabbit.port}")
    String rabbitPort;

    public Map<String, Object> getHealth() {
        String version = VersionHelper.getABVersion();

        Map<String, Object> healthResults = new HashMap<>();
        healthResults.put("elasticsearch", auditSearch.ping());
        healthResults.put("ab-search.version", version);
        String config = System.getProperty("ab.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("ab.integration");
        healthResults.put("ab.integration", integration);
        if (integration.equalsIgnoreCase("http")) {
            healthResults.put("abengine.result", abEngine);
        } else {
            healthResults.put("rabbitmq.host", rabbitHost);
            healthResults.put("rabbitmq.port", rabbitPort);
        }
        return healthResults;

    }


}
