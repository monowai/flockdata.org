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
    private String index = "";

    Map<String, Object> auditLinks = new HashMap<>();

    private String auditLink = null;

    protected TagInputBean() {
    }

    public TagInputBean(String tagName, String auditRelationship) {
        this(tagName, auditRelationship, null);
    }

    /**
     * Unique name by which this tag will be known
     * <p/>
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     * <p/>
     * This tag will not be associated with an AuditHeader (it has no auditRelationship)
     * <p/>
     * Code value defaults to the tag name
     *
     * @param tagName unique name
     */
    public TagInputBean(String tagName) {
        this();
        if (tagName.contains(":")) {
            String[] data = tagName.split(":");
            for (int i = 0; i < data.length; i++) {
                if (i == 0)
                    this.name = data[i];
                else {
                    if (data[i].contains(" "))
                        throw new RuntimeException("Tag Type cannot contain whitespace " + data[i]);

                    this.index = this.index + " :" + data[i];
                }

            }
            this.index = this.index.trim();
        } else
            this.name = tagName;

        this.code = this.name;
    }

    public TagInputBean(String tagName, String auditRelationship, Map<String, Object> relationshipProperties) {
        this(tagName);
        if (auditRelationship == null)
            auditRelationship = "general";
        else {
            auditRelationship = auditRelationship.trim();
            if (auditRelationship.contains(" "))
                throw new RuntimeException("Tag Type cannot contain whitespace [" + auditRelationship + "]");
        }

        addAuditLink(auditRelationship, relationshipProperties);

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

    public void setTargets(String tagRelationship, TagInputBean tagInputBean) {
        TagInputBean[] put = {tagInputBean};
        targets.put(tagRelationship, put);
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

    /**
     * Tag names cannot contain spaces and should begin with a single :
     * Will add the : if it is missing
     */

    public void setIndex(String index) {
        if (index != null && !"".equals(index) && !index.startsWith(":"))
            this.index = ":" + index;
        else
            this.index = index;
    }

    /**
     * Tag names cannot contain spaces and should begin with a single :
     * Will add the : if it is missing
     *
     * @return Colon prefixed name of the tag
     */
    public String getIndex() {
        return index;
    }

    /**
     * Associates this tag with the AuditHeader
     *
     * @param auditRelationship name of the relationship to the Audit Header
     * @param properties        properties to store against the relationship
     */
    public void addAuditLink(String auditRelationship, Map<String, Object> properties) {
        this.auditLinks.put(auditRelationship, properties);

    }

    public void addAuditLink(String auditRelationship) {
        addAuditLink(auditRelationship, null);
    }

    public Map<String, Object> getAuditLinks() {
        return auditLinks;
    }

    /**
     * @return name to relate this to an audit record
     */
    public String getAuditLink() {
        return auditLink;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagInputBean)) return false;

        TagInputBean that = (TagInputBean) o;

        if (reverse != that.reverse) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (index != null ? !index.equals(that.index) : that.index != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (reverse ? 1 : 0);
        result = 31 * result + (index != null ? index.hashCode() : 0);
        return result;
    }
}
