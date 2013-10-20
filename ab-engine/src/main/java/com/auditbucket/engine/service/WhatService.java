package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.repo.neo4j.model.AuditWhatNode;
import com.auditbucket.engine.repo.redis.RedisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
@Transactional
public class WhatService {
    static final ObjectMapper om = new ObjectMapper();
    public static final String NEO4J = "neo4j";
    public static final String REDIS = "redis";

    private static final ObjectMapper om = new ObjectMapper();
    private static final String NEO4J = "neo4j";
    private Logger logger = LoggerFactory.getLogger(WhatService.class);

    @Autowired(required = false)
    AuditDao auditDao = null;

    @Autowired
    RedisRepository redisRepository;

    public String logWhat(AuditChange change, String jsonText) {
    public String logWhat(AuditChange change, String jsonText, int version) {
        String store = change.getWhatStore();
        // ToDo: Enum?
        // ToDo: this is a Neo4J what node store
        if (store.equalsIgnoreCase(NEO4J)) {   // ToDo: add Redis store support
            return auditDao.save(change, jsonText, version);
        } else
        {
            // AuditChange will have to be saved with the remote store key
            AuditWhatNode what = new AuditWhatNode();
            what.setJsonWhat(jsonText);
            redisRepository.add(change.getAuditLog().getId(),what);
            return null;
        }
    }

    public AuditWhat getWhat(AuditChange change) {
        if (change == null || change.getWhat() == null)
            return null;
        String store = change.getWhatStore();
        // ToDo: this is a Neo4J what node store
        if (store.equalsIgnoreCase(NEO4J)) // ToDo: add Redis store support
            return auditDao.getWhat(Long.parseLong(change.getWhat().getId()));
        else
        {
            return redisRepository.getValue(change.getAuditLog().getId());
        }
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
        AuditWhat what = getWhat(compareFrom);

        if (what == null)
            return false;

        String jsonThis = what.getWhat();
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
