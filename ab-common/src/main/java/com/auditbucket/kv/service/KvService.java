package com.auditbucket.kv.service;

import com.auditbucket.track.bean.AuditDeltaBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.LogWhat;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 12:07 PM
 */
public interface KvService {
    String ping();

    void purge(String indexName);

    void doKvWrite(TrackResultBean resultBean) throws IOException;

    Log prepareLog(Log log, Map<String, Object> jsonText) throws IOException;

    LogWhat getWhat(Entity entity, Log log);

    void delete(Entity entity, Log change);

    boolean isSame(Entity entity, Log compareFrom, Map<String, Object> jsonWith);

    boolean isSame(String compareFrom, Map<String, Object> compareWith);

    AuditDeltaBean getDelta(Entity header, Log from, Log to);

    public enum KV_STORE {REDIS, RIAK}
}
