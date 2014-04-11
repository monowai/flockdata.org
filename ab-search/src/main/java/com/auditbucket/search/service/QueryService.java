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

package com.auditbucket.search.service;

import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.audit.model.TrackSearchDao;
import com.auditbucket.dao.AuditQueryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.stereotype.Service;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 9:03 PM
 */
@Service
@MessageEndpoint
public class QueryService  {
    @Autowired
    private TrackSearchDao auditSearch;

    @Autowired
    private AuditQueryDao auditQuery;

    private Logger logger = LoggerFactory.getLogger(QueryService.class);

    public void doSearch (String query, String index ){

    }

    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    public void delete(MetaHeader metaHeader) {
        auditSearch.delete(metaHeader, null);
    }

    public byte[] findOne(MetaHeader header) {
        return auditSearch.findOne(header);
    }

    public byte[] findOne(MetaHeader header, String id) {
        return auditSearch.findOne(header, id);
    }


}
