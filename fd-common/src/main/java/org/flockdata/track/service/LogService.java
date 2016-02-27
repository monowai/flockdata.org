/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.*;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;

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

    Future<Collection<TrackResultBean>> processLogs(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> processLogsSync(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, InterruptedException, ExecutionException, IOException;

    TrackResultBean writeLog(DocumentType documentType, Entity entity, ContentInputBean input, FortressUser fu) throws FlockException, IOException, ExecutionException, InterruptedException;

    EntityLog getLastLog(Entity entity) throws FlockException;

    StoredContent getContent(Entity entity, Log log);
}
