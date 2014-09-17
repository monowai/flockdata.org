package com.auditbucket.kv.service;

import com.auditbucket.track.bean.DeltaBean;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityContent;
import com.auditbucket.track.model.Log;

import java.io.IOException;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 12:07 PM
 */
public interface KvService {
    String ping();

    void purge(String indexName);

    void doKvWrite(TrackResultBean resultBean) throws IOException;

    Log prepareLog(Log log, ContentInputBean content) throws IOException;

    EntityContent getContent(Entity entity, Log log);

    void delete(Entity entity, Log change);

    boolean isSame(Entity entity, Log compareFrom, Log compareTo);

    boolean sameJson(EntityContent compareFrom, EntityContent compareWith);

    DeltaBean getDelta(Entity header, Log from, Log to);

    public enum KV_STORE {REDIS, RIAK}
}
