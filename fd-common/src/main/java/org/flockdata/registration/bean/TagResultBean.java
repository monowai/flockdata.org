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

package org.flockdata.registration.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Alias;

import java.util.ArrayList;

/**
 * Result after creating a tag
 *
 * Created by mike on 11/05/15.
 */
public class TagResultBean {
    String code;
    String name;
    String message;
    ArrayList<String> aliases = new ArrayList<>();
    private Tag tag =null;
    public TagResultBean(){}

    public TagResultBean(TagInputBean tagInputBean, Tag tag){

        this(tag);
        if ( tag == null ){
            this.code = tagInputBean.getCode();
            this.name = tagInputBean.getName();
        }
        if ( tagInputBean != null )
            this.message = tagInputBean.getServiceMessage();

    }

    public TagResultBean (Tag tag ) {
        this();
        this.tag = tag;
        if (tag != null) {
            this.code = tag.getCode();
            this.name = tag.getName();
            for (Alias alias : tag.getAliases()) {
                aliases.add(alias.getKey());
            }
        }
    }



    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getMessage() {
        return message;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ArrayList<String> getAliases() {
        return aliases;
    }

    @JsonIgnore
    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
