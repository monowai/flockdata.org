package com.auditbucket.kv.service;

import com.auditbucket.track.bean.AuditDeltaBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.MetaHeader;

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

    LogWhat getWhat(MetaHeader metaHeader, Log log);

    void delete(MetaHeader metaHeader, Log change);

    boolean isSame(MetaHeader metaHeader, Log compareFrom, Map<String, Object> jsonWith);

    boolean isSame(String compareFrom, Map<String, Object> compareWith);

    AuditDeltaBean getDelta(MetaHeader header, Log from, Log to);

    public enum KV_STORE {REDIS, RIAK}
}
