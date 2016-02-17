/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
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
    @RelatedTo(elementClass = Tag.class, type = "located")
    private Set<Tag> located = null;

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

        for (String label : labels) {
            if (!TagHelper.isInternalLabel(label))
//            if (!label.equals("_Tag") && !label.equals("Tag"))
                return label;
        }
        return "Tag";
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

    public Tag getLocated() {
        if (located == null || located.isEmpty())
            return null;

        return located.iterator().next();
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
