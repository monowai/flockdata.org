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
    private String type="";

    protected TagInputBean() {
    }
    public TagInputBean(String tagName, String type) {
        this(tagName);
        if ( type.contains(" "))
            throw new RuntimeException("Tag Type cannot contain whitespace");

        setType(":"+type);
    }

    /**
     * Unique name by which this tag will be known
     *
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     *
     * Code value defaults to the tag name
     *
     * @param tagName unique name
     */
    public TagInputBean(String tagName) {
        this();
        if ( tagName.contains(":")){
            String []data = tagName.split(":");
            for (int i = 0; i < data.length; i++) {
                if (i== 0)
                    this.name = data[i];
                else {
                    if ( data[i].contains(" "))
                        throw new RuntimeException("Tag Type cannot contain whitespace " +data[i]);

                    this.type= this.type +" :"+data[i];
                }

            }
            this.type = this.type.trim();
        } else
            this.name = tagName;

        this.code = this.name;
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


    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return Colon prefixed description of the tag
     */
    public String getType() {
        return type;
    }
}
