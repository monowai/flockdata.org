/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * All data necessary to create a simple Tag. If no tagLabel is provided then the tag is
 * created under the default tagLabel of _Tag
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 1:20 PM
 */
public class TagInputBean implements org.flockdata.transform.UserProperties {

    private String name;

    private String code;

    private boolean reverse = false;

    private Map<String, Collection<TagInputBean>> targets ;

    Map<String, Object> properties ;
    private String label = Tag.DEFAULT_TAG;

    Map<String, Map<String,Object>> entityLinks ;

    private String entityLink = null;
    private boolean mustExist = false;
    private String serviceMessage;
    private Collection<AliasInputBean> aliases;
    private String notFoundCode;
    private boolean since;
    private String keyPrefix=null;
    private boolean merge = false;


    public TagInputBean() {
    }

    /**
     * associates a tag to the Entity
     *
     * @param tagCode  Unique name for a tag (if exists will be reused)
     * @param tagLabel     The "type" of tag
     */
    public TagInputBean(String tagCode, String tagLabel) {
        this(tagCode, tagLabel, null);
    }

    /**
     * associates a tag to the entity giving it an optional tagLabel tagLabel to categorize it by
     *
     * @param tagCode                Unique name for a tag (if exists will be reused)
     * @param tagLabel               optional tagLabel tagLabel to give the Tag.
     * @param entityRelationshipName name of relationship to the Entity
     */
    public TagInputBean(String tagCode, String tagLabel, String entityRelationshipName) {
        this(tagCode, tagLabel, entityRelationshipName, null);


    }

    /**
     * Unique name by which this tag will be known
     * <p/>
     * You can pass this in as Name:Type and AB will additionally
     * recognize the tag as being of the supplied Type
     * <p/>
     * This tag will not be associated with an Entity (it has no tagLabel)
     * <p/>
     * Code value defaults to the tag name
     *
     * @param tagCode unique name
     */
    public TagInputBean(String tagCode) {
        this();
        if ( tagCode == null )
            throw new IllegalArgumentException("The code of a tag cannot be null");
        this.code= tagCode.trim();

        //this.code = this.name;
    }

    public TagInputBean(String tagCode, String tagLabel, String entityRelationshipName, Map<String, Object> relationshipProperties) {
        this(tagCode);
        setLabel(tagLabel);
        if (entityRelationshipName != null) {
            entityRelationshipName = entityRelationshipName.trim();
            if (entityRelationshipName.contains(" ")) {
                if (!entityRelationshipName.startsWith("'"))
                    entityRelationshipName = "'" + entityRelationshipName + "'";
                //   throw new RuntimeException("Tag Type cannot contain whitespace [" + tagLabel + "]");
            }
            addEntityLink(entityRelationshipName, relationshipProperties);
        }



    }

    public void setServiceMessage(String getServiceMessage) {
        this.serviceMessage = getServiceMessage;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String setServiceMessage() {
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
        if ( targets == null )
            targets = new HashMap<>();
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
        if ( mergeFrom.hasTargets()) {
            for (String next : mergeFrom.getTargets().keySet()) {
                setTargets(next, mergeFrom.getTargets().get(next));
            }
        }
        return this;
    }

    public Map<String, Collection<TagInputBean>> getTargets() {
        return this.targets;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void setProperty(String key, Object value) {
        if ( properties == null )
            properties = new HashMap<>();
        if ( key !=null && value != null )
            properties.put(key, value);
    }

    @Override
    public Object getProperty(String key){
        if (properties == null )
            return null;
        return properties.get(key);
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

    /**
     * Associates this tag with the Entity
     *
     * @param relationshipName name of the relationship to the Entity
     * @param properties        properties to store against the relationship
     */
    public TagInputBean addEntityLink(String relationshipName, Map<String, Object> properties) {
        if ( entityLinks == null )
            entityLinks = new HashMap<>();
        if ( entityLinks.get(relationshipName) == null )
            this.entityLinks.put(relationshipName, properties);
        return this;
    }

    public TagInputBean addEntityLink(String relationshipName) {
        if (relationshipName.equals("located"))
            setReverse(true);
        return addEntityLink(relationshipName, null);
    }

    public Map<String, Map<String,Object>> getEntityLinks() {
        if ( (entityLinks== null ||entityLinks.isEmpty()) && entityLink !=null )
            addEntityLink(entityLink);
        return entityLinks;
    }

    @Override
    public String toString() {
        return "TagInputBean{" +
                "label='" + label + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", keyPrefix='" + keyPrefix+ '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagInputBean)) return false;

        TagInputBean that = (TagInputBean) o;

        if (reverse != that.reverse) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (code != null ? !code.equalsIgnoreCase(that.code) : that.code != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        return !(keyPrefix != null ? !keyPrefix.equals(that.keyPrefix) : that.keyPrefix != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (reverse ? 1 : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (keyPrefix != null ? keyPrefix.hashCode() : 0);
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
        return label == null || Tag.DEFAULT_TAG.equals(label);
    }

    /**
     *
     *
     * @return Default tag tagLabel or the name to assign
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

    public boolean hasAliases() {
        return ( aliases!=null && !aliases.isEmpty());
    }

    public Collection<AliasInputBean> getAliases() {
        return aliases;
    }

    public void setAliases(Collection<AliasInputBean> aliases) {
        this.aliases = aliases;
    }

    @JsonIgnore
    /**
     * determines if this payload has the requested relationship. Does not walk the tree!!
     */
    public boolean hasRelationship(String relationship) {
        return (entityLink!=null && entityLink.equals(relationship)) || entityLinks.containsKey(relationship);
    }

    /**
     *
     * @param mustExist don't "just create" this tag
     * @param notFound  create and link to this tag if notfound
     * @return this
     */
    public TagInputBean setMustExist(boolean mustExist, String notFound) {
        setMustExist(mustExist);
        return setNotFoundCode(notFound);
    }

    public TagInputBean setNotFoundCode(String notFoundCode) {
        this.notFoundCode = notFoundCode;
        return this;
    }

    public String getNotFoundCode() {
        return notFoundCode;
    }

    public String getEntityLink() {
        return entityLink;
    }

    public TagInputBean setEntityLink(String entityLink) {
        this.entityLink = entityLink;
        return this;
    }


    public TagInputBean setSince(boolean since) {
        this.since = since;
        return this;
    }

    public boolean isSince() {
        return since;
    }

    public void addAlias(AliasInputBean alias) {
        if ( aliases == null )
            aliases = new ArrayList<>();
        aliases.add(alias);
    }

    @JsonIgnore
    // Determines if a valid NotFoundCode is set
    public boolean hasNotFoundCode() {
        return !(notFoundCode == null  || notFoundCode.equals(""));
    }

    /**
     * Codes can have duplicate values in a Label but the key must be unique.
     * When being created, the code is used as part of the key. Setting this property
     * will prefix the key with this value.
     *
     * Useful in geo type scenarios. Take Cambridge - a city in England and in New Zealand
     *
     * With a tagPrefix we have uk.cambridge and nz.cambridge both with a code of Cambridge. Each
     * tag is distinct but they share human readable properties
     *
     * @param keyPrefix  default is not set.
     */
    public TagInputBean setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public TagInputBean setMerge(boolean merge) {
        this.merge = merge;
        return this;
    }

    /**
     * Default == false
     * if true, then properties in this payload will be added to an existing tag
     * @return caller wants to merge properties
     */
    public boolean isMerge() {
        return merge;
    }

    public boolean contains(String code, String label, String keyPrefix) {
       return findTargetTag(code, label, keyPrefix) !=null;
    }

    /**
     * determines if the targets Map contains a TagInputBean with requested properties
     * Tags are uniquely identified by either Code or keyPrefix+"-"+code within a Label
     *
     * @since DAT-491
     * @param code      case-insensitive - mandatory
     * @param label     case-sensitive - mandatory
     * @param keyPrefix case-insensitive - optional
     * @return
     */
    public TagInputBean findTargetTag(String code, String label, String keyPrefix) {
        if ( !hasTargets())
            return null;

        for (String key : targets.keySet()) {
            for (TagInputBean tagInputBean : targets.get(key)) {
                if ( tagInputBean.getCode().equalsIgnoreCase(code) && tagInputBean.getLabel().equals(label)) {
                    if ( keyPrefix == null || keyPrefix.length() ==0 )
                        return tagInputBean;// Ignoring the keyPrefix
                    if ( tagInputBean.getKeyPrefix().equalsIgnoreCase(keyPrefix))
                        return tagInputBean;
                }
                TagInputBean found = tagInputBean.findTargetTag(code, label, keyPrefix);
                if ( found!=null )
                    return found;
            }
        }
        return null;
    }

    public boolean contains(String code, String label) {
        return contains(code, label, null);
    }

    public boolean hasTargets() {
        return this.targets!=null && !targets.isEmpty();
    }

    public boolean hasTagProperties() {
        return this.properties!=null && !properties.isEmpty();
    }



}
