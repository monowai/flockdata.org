/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;

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

    TrackResultBean writeLog(Entity entity, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    EntityLog getLastLog(Entity entity) throws FlockException;

}
