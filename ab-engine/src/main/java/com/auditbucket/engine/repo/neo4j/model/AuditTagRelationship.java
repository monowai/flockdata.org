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

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TagValue;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.springframework.data.neo4j.annotation.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */

@RelationshipEntity(type = "tagValue")
public class AuditTagRelationship implements TagValue {
    @GraphId
    Long id;

    @StartNode
    @Fetch
    TagNode tag;

    @EndNode
    AuditHeaderNode auditHeader;

    @Indexed(indexName = "tagValue")
    String tagValue;

    protected AuditTagRelationship() {
    }

    public AuditTagRelationship(Tag tag, AuditHeader header, String tagValue) {
        this.tag = (TagNode) tag;
        this.auditHeader = (AuditHeaderNode) header;
        this.tagValue = tagValue;
    }

    @Override
    public Tag getTag() {
        return tag;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public AuditHeader getHeader() {
        return auditHeader;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getTagValue() {
        return tagValue;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
}
