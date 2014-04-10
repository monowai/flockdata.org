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
 * All data necessary to create a simple Tag. If no index is provided then the tag is
 * created under the default index of _Tag
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

    Map<String, Object> properties = new HashMap<>();
    private String index = "";

    Map<String, Object> metaLinks = new HashMap<>();

    private String metaLink = null;
    private boolean mustExist = false;
    private String serviceMessage;


    protected TagInputBean() {
    }

    /**
     * associates a tag to the Meta Header
     *
     * @param tagName              Unique name for a tag (if exists will be reused)
     * @param metaRelationshipName name of relationship to the MetaHeader
     */
    public TagInputBean(String tagName, String metaRelationshipName) {
        this(tagName, null, metaRelationshipName);
    }

    /**
     * associates a tag to the meta header giving it an optional index label to categorize it by
     *
     * @param tagName              Unique name for a tag (if exists will be reused)
     * @param index                optional index label to give the Tag. Must start with ":"
     * @param metaRelationshipName name of relationship to the MetaHeader
     */
    public TagInputBean(String tagName, String index, String metaRelationshipName) {
        this(tagName, metaRelationshipName, (Map<String, Object>) null);
        setIndex(index);

    }

    /**
     * Unique name by which this tag will be known
     * <p/>
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     * <p/>
     * This tag will not be associated with an MetaHeader (it has no metaRelationshipName)
     * <p/>
     * Code value defaults to the tag name
     *
     * @param tagName unique name
     */
    public TagInputBean(String tagName) {
        this();
        this.name = tagName;

        this.code = this.name;
    }

    public TagInputBean(String tagName, String metaRelationshipName, Map<String, Object> relationshipProperties) {
        this(tagName);
        if (metaRelationshipName == null)
            metaRelationshipName = "general";
        else {
            metaRelationshipName = metaRelationshipName.trim();
            if (metaRelationshipName.contains(" ")) {
                if (!metaRelationshipName.startsWith("'"))
                    metaRelationshipName = "'" + metaRelationshipName + "'";
                //   throw new RuntimeException("Tag Type cannot contain whitespace [" + metaRelationshipName + "]");
            }
        }

        addMetaLink(metaRelationshipName, relationshipProperties);

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
        ArrayList<TagInputBean> val = new ArrayList<>() ;
        val.add(tagInputBean);
        setTargets(tagRelationship, val);

    }

    public void setTargets(String relationshipName, Collection<TagInputBean> fromThoseTags) {
        Collection<TagInputBean> theseTags = targets.get(relationshipName);
        if ( theseTags == null )
            targets.put(relationshipName, fromThoseTags);
        else {
            for (TagInputBean tagToAdd : fromThoseTags) {
                if ( !theseTags.contains(tagToAdd))
                    theseTags.add(tagToAdd);
            }
        }

    }

    public void mergeTags (TagInputBean mergeFrom){
        for (String next : mergeFrom.getTargets().keySet()) {
            setTargets(next, mergeFrom.getTargets().get(next));
        }

    }

    public Map<String, Collection<TagInputBean>> getTargets() {
        return this.targets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperty(String key, Serializable value) {
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
     * Index name cannot contain spaces and will begin with a single :
     * Will add the leading : if it is missing
     */
    public void setIndex(String index) {
        if (index == null)
            return;

        if (!index.startsWith(":"))
            this.index = ":" + index.trim();
        else
            this.index = index.trim();

//        if (index.contains(":")) {
//            String[] data = index.split(":");
//            for (String aData : data) {
//                isValid(aData);
//                if (!"".equals(aData))
//                    this.index = this.index + ":" + aData +" ";
//
//            }
//            this.index = this.index.trim();
//        } else {
//            isValid(parseIndex);
//            this.index = parseIndex;
//        }

    }

    private void isValid(String aData) {
        if (aData.contains(" "))
            throw new RuntimeException("Tag Type cannot contain whitespace " + aData);
    }

    /**
     * Indexes should not contain spaces and should begin with a single :
     *
     * @return Colon prefixed name of the tag
     */

    public String getIndex() {
        if ( "".equals(index) )
            return ":_Tag";
        else
            return index;
    }

    /**
     * Associates this tag with the MetaHeader
     *
     * @param relationshipName name of the relationship to the Audit Header
     * @param properties        properties to store against the relationship
     */
    public void addMetaLink(String relationshipName, Map<String, Object> properties) {
        if ( metaLinks.get(relationshipName) == null )
            this.metaLinks.put(relationshipName, properties);

    }

    public void addMetaLink(String relationshipName) {
        addMetaLink(relationshipName, null);
    }

    public Map<String, Object> getMetaLinks() {
        return metaLinks;
    }

    /**
     * @return name to relate this to an audit record
     */
    public String getMetaLink() {
        return metaLink;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", index='" + index + '\'' +
                ", targets=" + targets.keySet().size() +
                ", metaLinks=" + metaLinks +
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

    public boolean isMustExist() {
        return mustExist;
    }

    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }

    @JsonIgnore
    public boolean isDefault() {
        return index == null || "".equals(index);
    }
}
