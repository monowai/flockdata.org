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

package com.auditbucket.registration.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 1:20 PM
 */
public class TagInputBean {

    @NotEmpty
    private String name;

    private String code;

    private boolean reverse = false;

    private Map<String, TagInputBean[]> targets = new HashMap<>();

    Map<String, Object> properties = new HashMap<>();

    protected TagInputBean() {
    }

    public TagInputBean(String tagName) {
        this();
        this.name = tagName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Long getId() {
        return null;
    }

    public String getCode() {
        return code;
    }

    public void setTargets(String relationshipName, TagInputBean tagInputBean) {
        TagInputBean[] put = {tagInputBean};
        targets.put(relationshipName, put);
    }

    public void setTargets(String relationshipName, TagInputBean[] tagInputBeans) {
        targets.put(relationshipName, tagInputBeans);
    }

    public Map<String, TagInputBean[]> getTargets() {
        return this.targets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperty(String key, Object value) {
        if (!key.contains("id") | key.contains("name"))
            properties.put(key, value);
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
