package com.auditbucket.track.service;

import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:23 PM
 */
public interface LogService {

    Collection<TrackResultBean> processLogsSync(Company company, Iterable<TrackResultBean> resultBeans) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean writeLog(Entity entity, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    EntityLog getLastLog(Entity entity) throws FlockException;

}
