/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.service;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.helper.FlockException;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * @author mholdsworth
 * @since 5/09/2014
 */
public interface LogService {

  Future<Collection<TrackResultBean>> processLogs(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, ExecutionException, InterruptedException;

  Collection<TrackResultBean> processLogsSync(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, InterruptedException, ExecutionException;

  TrackResultBean writeLog(Document documentType, Entity entity, ContentInputBean input, FortressUser fu) throws FlockException, IOException, ExecutionException, InterruptedException;

  EntityLog getLastLog(Entity entity) throws FlockException;

  StoredContent getContent(Entity entity, LogNode log);

//    Log prepareLog (Store defaultStore, TrackResultBean trackResult, Log log);


}
