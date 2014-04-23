/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.track.bean;

import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.TrackLog;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
public class LogDetailBean {

    private LogDetailBean() {
    }

    public LogDetailBean(TrackLog log, LogWhat what) {
        this();
        this.log = log;
        this.what = what;

    }

    private TrackLog log;
    private LogWhat what;

    public TrackLog getLog() {
        return this.log;
    }

    public LogWhat getWhat() {
        return this.what;
    }

}