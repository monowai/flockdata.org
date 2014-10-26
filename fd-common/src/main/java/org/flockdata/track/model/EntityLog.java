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

package org.flockdata.track.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * User: Mike Holdsworth
 * Date: 21/06/13
 * Time: 1:21 PM
 */
public interface EntityLog {

    Long getId();

    public Log getLog();

    public Entity getEntity();

    public boolean isIndexed();

    /**
     * flags this track as having been indexed at some point.
     */
    public void setIsIndexed();

    /**
     * @return When this log file was created in FlockData
     */
    public Long getSysWhen();

    public Long getFortressWhen();

    void setEntity(Entity entity);

    DateTime getFortressWhen(DateTimeZone tz);
}
