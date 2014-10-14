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

package com.auditbucket.registration.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * All data necessary to create a simple Tag. If no label is provided then the tag is
 * created under the default label of _Tag
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 1:20 PM
 */
public class TagInputBean {

    @NotEmpty
    private String name;

    private String code;

    private boolean reverse = false;

    private Map<String, Collection<TagInputBean>> targets = new HashMap<>();

    Map<String, Object> properties = null;
    private String label = "";

    @Deprecated
    private String index = "";

    Map<String, Object> entityLinks = new HashMap<>();

    private String entityLink = null;
    private boolean mustExist = false;
    private String serviceMessage;


    public TagInputBean() {
    }

    /**
     * associates a tag to the Entity
     *
     * @param tagValue              Unique name for a tag (if exists will be reused)
     * @param entityRelationshipName name of relationship to the Entity
     */
    public TagInputBean(String tagValue, String entityRelationshipName) {
        this(tagValue, null, entityRelationshipName);
    }

    /**
     * associates a tag to the entity giving it an optional label label to categorize it by
     *
     * @param tagValue              Unique name for a tag (if exists will be reused)
     * @param tagLabel                optional label label to give the Tag. Must start with ":"
     * @param entityRelationshipName name of relationship to the Entity
     */
    public TagInputBean(String tagValue, String tagLabel, String entityRelationshipName) {
        this(tagValue, entityRelationshipName, (Map<String, Object>) null);
        setLabel(tagLabel);

    }

    /**
     * Unique name by which this tag will be known
     * <p/>
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     * <p/>
     * This tag will not be associated with an Entity (it has no entityRelationshipName)
     * <p/>
     * Code value defaults to the tag name
     *
     * @param tagValue unique name
     */
    public TagInputBean(String tagValue) {
        this();
        if ( tagValue == null )
            throw new IllegalArgumentException("The name of a tag cannot be null");
        this.name = tagValue;

        this.code = this.name;
    }

    public TagInputBean(String tagName, String entityRelationshipName, Map<String, Object> relationshipProperties) {
        this(tagName);
        if (entityRelationshipName == null)
            entityRelationshipName = "general";
        else {
            entityRelationshipName = entityRelationshipName.trim();
            if (entityRelationshipName.contains(" ")) {
                if (!entityRelationshipName.startsWith("'"))
                    entityRelationshipName = "'" + entityRelationshipName + "'";
                //   throw new RuntimeException("Tag Type cannot contain whitespace [" + entityRelationshipName + "]");
            }
        }

        addEntityLink(entityRelationshipName, relationshipProperties);

    }

    public void getServiceMessage(String getServiceMessage) {
        this.serviceMessage = getServiceMessage;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getServiceMessage() {
        return serviceMessage;
    }

    public String getName() {
        return name;
    }

    public TagInputBean setName(String name) {
        this.name = name;
        return this;
    }

    @JsonIgnore
    public Long getId() {
        return null;
    }

    public String getCode() {
        return code;
    }

    public TagInputBean setTargets(String tagRelationship, TagInputBean tagInputBean) {
        ArrayList<TagInputBean> val = new ArrayList<>() ;
        val.add(tagInputBean);
        setTargets(tagRelationship, val);
        return this;

    }

    public TagInputBean setTargets(String relationshipName, Collection<TagInputBean> fromThoseTags) {
        Collection<TagInputBean> theseTags = targets.get(relationshipName);
        if ( theseTags == null )
            targets.put(relationshipName, fromThoseTags);
        else {
            for (TagInputBean tagToAdd : fromThoseTags) {
                if ( !theseTags.contains(tagToAdd))
                    theseTags.add(tagToAdd);
            }
        }
        return this;

    }

    public TagInputBean mergeTags (TagInputBean mergeFrom){
        for (String next : mergeFrom.getTargets().keySet()) {
            setTargets(next, mergeFrom.getTargets().get(next));
        }
        return this;
    }

    public Map<String, Collection<TagInputBean>> getTargets() {
        return this.targets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public TagInputBean setProperty(String key, Serializable value) {
        if ( properties == null )
            properties = new HashMap<>();
        properties.put(key, value);
        return this;
    }

    public boolean isReverse() {
        return reverse;
    }

    public TagInputBean setReverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public TagInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    @Deprecated
    // Use setLabel
    public TagInputBean setIndex(String index){
        return setLabel(index);
    }
    /**
     * Index name cannot contain spaces and will begin with a single :
     * Will add the leading : if it is missing
     */
    public TagInputBean setLabel(String label) {
        if (label == null)
            return this;

        if (!label.startsWith(":"))
            this.label = ":" + label.trim();
        else
            this.label = label.trim();

        return this;

    }

    private void isValid(String aData) {
        if (aData.contains(" "))
            throw new RuntimeException("Tag Type cannot contain whitespace " + aData);
    }

    /**
     * Associates this tag with the Entity
     *
     * @param relationshipName name of the relationship to the Entity
     * @param properties        properties to store against the relationship
     */
    public TagInputBean addEntityLink(String relationshipName, Map<String, Object> properties) {
        if ( entityLinks.get(relationshipName) == null )
            this.entityLinks.put(relationshipName, properties);
        return this;
    }

    public TagInputBean addEntityLink(String relationshipName) {
        return addEntityLink(relationshipName, null);
    }

    public Map<String, Object> getEntityLinks() {
        if ( entityLinks.isEmpty() && entityLink !=null )
            addEntityLink(entityLink);
        return entityLinks;
    }

    /**
     * @return name to relate this to an track record
     */
    public String getEntityLink() {
        return entityLink;
    }

    @Deprecated
    // Since 0.97 use setEntityLink
    public void setMetaLink(String link){
        this.entityLink = link;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", targets=" + targets.keySet().size() +
                ", entityLinks=" + entityLinks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagInputBean)) return false;

        TagInputBean that = (TagInputBean) o;

        if (reverse != that.reverse) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (reverse ? 1 : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public TagInputBean setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
        return this;
    }

    @JsonIgnore
    public boolean isDefault() {
        return label == null || "".equals(label);
    }

    /**
     *
     *
     * @return Default tag label or the name to assign
     */
    private String getLabelValue() {
        if ( "".equals(label) )
            return "_Tag";
        else
            return label;
    }

    public String getLabel() {
        String thisLabel = getLabelValue();
        if ( thisLabel.startsWith(":"))
            thisLabel= thisLabel.substring(1, thisLabel.length());
        return thisLabel;
    }
}
