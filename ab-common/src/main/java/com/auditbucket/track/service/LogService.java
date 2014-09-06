package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:23 PM
 */
public interface LogService {

    Collection<TrackResultBean> processLogsSync(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean writeLog(MetaHeader metaHeader, LogInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackLog getLastLog(MetaHeader metaHeader) throws DatagioException;

}
