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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 9:11 PM
 */
@NodeEntity // Only in place to support projection
@TypeAlias("Tag")
public class Tag {

    public static final String DEFAULT_TAG = "Tag";
    public static final String DEFAULT = ":" + DEFAULT_TAG;
    public static final String UNDEFINED = "undefined";
    public static final String PROPS_PREFIX = "props-";
    public static final String LAT = "latitude";
    public static final String LON = "longitude";
    public static final String NODE_LAT = PROPS_PREFIX + LAT;
    public static final String NODE_LON = PROPS_PREFIX + LON;

    @GraphId
    Long id;

    @Indexed
    private String key;

    @Indexed
    private String code;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();

    //@Relationship(type = "HAS_ALIAS")
    @RelatedTo(elementClass = Alias.class, type = "HAS_ALIAS")
    private Set<Alias> aliases = new HashSet<>();

    //@Relationship(type = "located")
//    @RelatedTo(elementClass = Tag.class, type = "located")
//    private Set<Tag> located = null;

    DynamicProperties props = new DynamicPropertiesContainer();

    private String name;

    @Transient
    private Boolean isNew = false;

    protected Tag() {
        labels.add("Tag");
        labels.add("_Tag"); // Required for SDN 3.x
    }

    public Tag(TagInputBean tagInput) {
        this();
        setName(tagInput.getName());
        if (tagInput.getCode() == null)
            setCode(getName());
        else
            setCode(tagInput.getCode());

        this.key = TagHelper.parseKey(tagInput);
        if (tagInput.hasTagProperties()) {
            props = new DynamicPropertiesContainer(tagInput.getProperties());
        }
        String label = tagInput.getLabel();
        if (label != null) {
            this.labels.add(label);
        }
    }

    // Called only when creating a new Tag
    public Tag(TagInputBean tagInput, String tagLabel) {
        this(tagInput);
        if (!labels.contains(tagLabel))
            labels.add(tagLabel);
        isNew = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return "TagNode{" +
                "id=" + id +
                ", label='" + getLabel() + '\'' +
                ", code='" + code + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    public String getKey() {
        return key;
    }

    public Object getProperty(String name) {
        return props.getProperty(name);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return props.asMap();
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return TagHelper.getLabel(labels);
    }

    public void setId(Long id) {
        this.id = id;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;

        Tag tagNode = (Tag) o;

        if (id != null ? !id.equals(tagNode.id) : tagNode.id != null) return false;
        if (code != null ? !code.equals(tagNode.code) : tagNode.code != null) return false;
        if (key != null ? !key.equals(tagNode.key) : tagNode.key != null) return false;
        return !(name != null ? !name.equals(tagNode.name) : tagNode.name != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public void addAlias(Alias newAlias) {
        aliases.add(newAlias);
    }

    public boolean hasAlias(String theLabel, String code) {
        if (aliases.isEmpty())
            return false;
        for (Alias alias : aliases) {
            if (alias.getKey().equals(code) && alias.getLabel().equals(theLabel + "Alias"))
                return true;
        }
        return false;
    }

    public Set<Alias> getAliases() {
        return aliases;
    }

    @JsonIgnore
    public boolean isDefault() {
        return getLabel() == null || DEFAULT_TAG.equals(getLabel());
    }

    @JsonIgnore
    public Boolean isNew() {
        return isNew;
    }


    public void addProperty(String key, Object property) {
        props.setProperty(key, property);
        //getProperties().put(key, property);
    }

    @Transient
    Map<String, Collection<Tag>> subTags = new HashMap<>();

    @JsonIgnore
    public void addSubTag(String key, Collection<Tag> o) {
        subTags.put(key, o);
    }

    @JsonIgnore
    public Map<String, Collection<Tag>> getSubTags() {
        return subTags;
    }

    @JsonIgnore
    public Collection<Tag> getSubTags(String key) {
        return subTags.get(key);
    }

    public boolean hasSubTags() {
        return (subTags != null && !subTags.isEmpty());
    }

    public boolean hasProperties() {
        return(getProperties() != null && !getProperties().isEmpty());
    }
}
