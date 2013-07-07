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

package com.auditbucket.audit.model;

import java.util.Date;
import java.util.Map;

/**
 * User: mike
 * Date: 21/04/13
 * Time: 7:44 PM
 */
public interface IAuditChange {

    public void setWhat(Map<String, Object> what);

    public Map<String, Object> getWhat();

    public Map<String, Object> getTagValues();

    public String getWho();

    public String getFortressName();

    public String getCompanyName();

    public String getIndexName();

    public Date getWhen();

    public void setId(String id);

    public void setVersion(long version);

    public Long getVersion();

    public String getName();

    public String getId();

    public void setSearchKey(String parent);

    public String getSearchKey();

    void setName(String event);

    void setWhen(Date date);

    void setWho(String name);

    String getAuditKey();

    String getRoutingKey();

    String getDocumentType();

}
