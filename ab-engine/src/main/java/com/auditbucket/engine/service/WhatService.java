package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.repo.redis.RedisRepository;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static final String REDIS = "redis";
    private static final ObjectMapper om = new ObjectMapper();
    @Autowired(required = false)
    AuditDao auditDao = null;
    @Autowired
    RedisRepository redisRepository;
    private Logger logger = LoggerFactory.getLogger(WhatService.class);

    public String logWhat(AuditChange change, String jsonText, int version) {
        // Compress the Value of JSONText
        CompressionResult result = CompressionHelper.compress(jsonText);
        Boolean compressed = result.getMethod() == CompressionResult.Method.GZIP;

        // Store First all information In Neo4j
        change = auditDao.save(change, compressed, version);

        // Store the what information Compressed in KV Store Depending on

        String store = change.getWhatStore();
        if (store.equalsIgnoreCase(REDIS)) {
            redisRepository.add(change.getId(), result.getAsBytes());
        } else {
            throw new IllegalStateException("The only supported KV Store is REDIS");
        }
        return change.getWhat().getId();
    }

    public AuditWhat getWhat(AuditChange change) {
        if (change == null || change.getWhat() == null)
            return null;
        String store = change.getWhatStore();
        // ToDo: this is a Neo4J what node store

        AuditWhat auditWhat = auditDao.getWhat(Long.parseLong(change.getWhat().getId()));
        if (store.equalsIgnoreCase(REDIS)) {
            byte[] whatInformation = redisRepository.getValue(change.getId());
            auditWhat.setWhatBytes(whatInformation);
        } else {
            throw new IllegalStateException("The only supported KV Store is REDIS");
        }
        return auditWhat;
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
