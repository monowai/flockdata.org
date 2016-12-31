/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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
import org.flockdata.data.Concept;
import org.flockdata.track.bean.AliasResultBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Public facing view of a results of a tag track result
 *
 * @tag Tag, Json
 * @author mholdsworth
 * @since 11/05/2015
 */
public class TagResultBean {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected
    String code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String key;
    protected String label;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String message;

    @JsonIgnore
    protected Boolean newTag = false;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected ArrayList<AliasResultBean> aliases = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected Map<String,Object> properties = new HashMap<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String relationship;

    public TagResultBean(){}

    public TagResultBean(String code, String name, String label) {
        this.code = code;
        this.name = name;
        this.label = label;
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


    public String getDescription() {
        return description;
    }


}
