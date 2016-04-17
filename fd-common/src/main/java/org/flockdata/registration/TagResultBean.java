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
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Tag;
import org.flockdata.track.bean.AliasResultBean;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Result after creating a tag
 *
 * Created by mike on 11/05/15.
 */
public class TagResultBean {
    String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String key;
    String label;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String message;

    Boolean newTag = false;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    ArrayList<AliasResultBean> aliases = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String,Object> properties = new HashMap<>();
    @JsonIgnore
    private Tag tag =null;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String relationship;

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
        if ( tagInputBean != null )
            this.message = tagInputBean.setServiceMessage();

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
    @JsonIgnore
    public boolean isNew() {
        return newTag;
    }

    void setNew(){
        this.newTag = true;
    }

    @Override
    public String toString() {
        return "TagResultBean{" +
                "label='" + label + '\'' +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getRelationship() {
        return relationship;
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
}
