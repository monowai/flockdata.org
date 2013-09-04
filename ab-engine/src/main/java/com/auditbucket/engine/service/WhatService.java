package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.dao.AuditDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
public class WhatService {
    static final ObjectMapper om = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(WhatService.class);

    @Autowired(required = false)
    AuditDao auditDao = null;

    public String logWhat(AuditChange change, String jsonText) {
        String store = change.getWhatStore();
        // ToDo: Enum?
        if (store.equalsIgnoreCase("neo4j"))   // ToDo: add Redis store support
            return auditDao.save(change, jsonText);
        else
            return null;
    }

    public AuditWhat getWhat(AuditChange change) {
        // ToDo: this is a Neo4J what node store
        if (change == null)
            return null;
        String store = change.getWhatStore();
        if (store.equalsIgnoreCase("neo4j")) // ToDo: add Redis store support
            return auditDao.getWhat(Long.parseLong(change.getWhat().getId()));
        else
            return null;
    }

    /**
     * Locate and compare the two JSON What documents to determine if they have changed
     *
     * @param compareFrom existing change to compare from
     * @param compareWith new Change to compare with - JSON format
     * @return false if different, true if same
     */
    public boolean isSame(AuditChange compareFrom, String compareWith) {
        if (compareFrom == null)
            return false;

        String jsonThis = getWhat(compareFrom).getWhat();
        if (jsonThis == null || compareWith == null)
            return false;

        if (jsonThis.length() != compareWith.length())
            return false;

        // Compare values
        JsonNode compareTo = null;
        JsonNode other = null;
        try {
            compareTo = om.readTree(jsonThis);
            other = om.readTree(compareWith);
        } catch (IOException e) {
            logger.error("Comparing JSON docs");
        }
        return !(compareTo == null || other == null) && compareTo.equals(other);

    }

    public String getDelta(Long sourceId, Long otherId) {
        // ToDo: obtain the delta
        return null;
    }
}
