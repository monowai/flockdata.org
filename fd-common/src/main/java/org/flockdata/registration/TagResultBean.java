/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Concept;
import org.flockdata.model.Tag;
import org.flockdata.track.bean.AliasResultBean;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Result after creating a tag
 *
 * @author mholdsworth
 * @since 11/05/2015
 */
public class TagResultBean {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String key;
    String label;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String message;

    @JsonIgnore
    Boolean newTag = false;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    ArrayList<AliasResultBean> aliases = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String,Object> properties = new HashMap<>();
    Map<TagResultBean, Collection<String>> targets = new HashMap<>();
    @JsonIgnore
    private Tag tag =null;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String relationship;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    public TagResultBean(){}

    public TagResultBean(TagInputBean tagInput, Tag startTag, boolean isNew) {
        this(tagInput, startTag);
        this.newTag = isNew;
    }


    public TagResultBean(TagInputBean tagInputBean, Tag tag){

        this(tag);
        if ( tag == null ){
            this.code = tagInputBean.getCode();
            this.name = tagInputBean.getName();
        }
        if ( tagInputBean != null ) {
            this.message = tagInputBean.setServiceMessage();
            this.description = tagInputBean.getDescription();
        }

    }

    public TagResultBean (Tag tag ) {
        this();
        this.tag = tag;

        if (tag != null) {
            this.newTag = tag.isNew();
            this.code = tag.getCode();
            this.key = tag.getKey();
            this.name = tag.getName();
            this.label = tag.getLabel();
            if (code.equals(name))
                name = null;
            this.properties = tag.getProperties();

            for (Alias alias : tag.getAliases()) {
                aliases.add(new AliasResultBean(alias));
            }
        }
    }

    public TagResultBean(TagInputBean tagInput) {
        this(tagInput, null);
    }

    public TagResultBean(Node pc) {
        this.code= pc.getProperty("code").toString();
        if ( pc.hasProperty("name"))
            this.name = pc.getProperty("name").toString();
        this.label = TagHelper.getLabel(pc.getLabels());
    }

    public TagResultBean(Concept concept) {
        this.label = concept.getName();
        this.description = concept.getDescription();
    }

    public String getKey() {
        return key;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<AliasResultBean> getAliases() {
        return aliases;
    }

    public String getLabel() {
        return label;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    // Used as a hint to see if we should attempt to create a TagLabel for this tag
    public boolean isNewTag() {
        return newTag;
    }

    @Override
    public String toString() {
        return "TagResultBean{" +
                "label='" + label + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagResultBean)) return false;

        TagResultBean that = (TagResultBean) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return label != null ? label.equals(that.label) : that.label == null;

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    public void addTargetResult(String rlxName, TagResultBean targetTag) {
        Collection<String>relationships = targets.get(targetTag);
        if ( relationships == null )
            relationships = new ArrayList<>();
        relationships.add(rlxName);
        targets.put(targetTag,relationships);
    }

    @JsonIgnore
    public Map<TagResultBean, Collection<String>> getTargets() {
        return targets;
    }

    public String getDescription() {
        return description;
    }
}
