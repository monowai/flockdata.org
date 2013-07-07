/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.search;

import com.auditbucket.audit.model.IAuditHeader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 22/06/13
 * Time: 6:28 AM
 */
public class SearchDocumentBean {
    private IAuditHeader auditHeader;
    private DateTime dateTime;
    private Map<String, Object> what;
    private String event;
    private static final ObjectMapper om = new ObjectMapper();

    public SearchDocumentBean(IAuditHeader auditHeader, DateTime dateTime, String what, String event) throws IOException {
        this.auditHeader = auditHeader;
        this.dateTime = dateTime;
        this.event = event;
        om.readValue(what, Map.class);
    }


    public IAuditHeader getAuditHeader() {
        return auditHeader;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public Map<String, Object> getWhat() {
        return what;
    }

    public String getEvent() {
        return event;
    }
}
