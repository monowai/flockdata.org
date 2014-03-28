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

package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.engine.PropertyConversion;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Tag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neo4j.graphdb.Node;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:35 PM
 */
@NodeEntity // Only in place to support projection
@TypeAlias("Tag")
public class TagNode implements Tag {
    @GraphId
    Long id;

    private String key;

    private String code;

    Map<String,Object> properties = new HashMap<>();

    private String name;

    protected TagNode() {
    }

    public TagNode(TagInputBean tagInput) {
        this();
        setName(tagInput.getName());
        if (tagInput.getCode() == null)
            setCode(getName());
        else
            setCode(tagInput.getCode());

        this.key = getCode().toLowerCase().replaceAll("\\s", "");
        if ( !tagInput.getProperties().isEmpty())
            properties.putAll(tagInput.getProperties());
    }
    @Deprecated
    public TagNode(Node tag) {
        this.id = tag.getId();
        this.code = (String) tag.getProperty("code");
        this.name = (String) tag.getProperty("name");
        this.key = (String) tag.getProperty("key");
        Iterable<String> keys = tag.getPropertyKeys();
        for (String s : keys) {
            if ( !(PropertyConversion.isSystemColumn(s)))
                properties.put(s, tag.getProperty(s));

        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String tagName) {
        this.name = tagName;
    }

    @JsonIgnore
    public Long getId() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return "TagNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @JsonIgnore
    public String getKey() {
        return key;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() {
        return code;
    }


    public void setId(Long id) {
        this.id = id;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagNode)) return false;

        TagNode tagNode = (TagNode) o;

        if (id != null ? !id.equals(tagNode.id) : tagNode.id != null) return false;
        if (code != null ? !code.equals(tagNode.code) : tagNode.code != null) return false;
        if (key != null ? !key.equals(tagNode.key) : tagNode.key != null) return false;
        if (name != null ? !name.equals(tagNode.name) : tagNode.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
