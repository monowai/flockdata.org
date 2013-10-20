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
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */

public class AuditTagRelationship implements AuditTag {
    Long id;

    private TagNode tag;

    private Long auditId;

    private String tagType;

    protected AuditTagRelationship() {
    }

    public AuditTagRelationship(AuditHeader header, Tag tag, Relationship tagType) {
        this();
        this.auditId = header.getId();
        this.tag = (TagNode) tag;
        this.tagType = (tagType == null ? tag.getName() : tagType.getType().name());
        this.id = tagType.getId();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Tag getTag() {
        return tag;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @JsonIgnore
    public Long getAuditId() {
        return auditId;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JsonIgnore
    public String getTagType() {
        return tagType;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditTagRelationship)) return false;

        AuditTagRelationship that = (AuditTagRelationship) o;

        if (!auditId.equals(that.auditId)) return false;
        if (!id.equals(that.id)) return false;
        if (!tag.getId().equals(that.tag)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + tag.getId().hashCode();
        result = 31 * result + auditId.hashCode();
        return result;
    }
}
