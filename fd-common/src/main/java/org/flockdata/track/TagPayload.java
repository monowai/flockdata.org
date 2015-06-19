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

package org.flockdata.track;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 19/06/15.
 */
public class TagPayload {

    String suffix ="";
    boolean ignoreRelationships;
    Collection<TagInputBean>tags;
    Company company;

    TagPayload (){}
    public TagPayload (Company company){this.company = company;}

    public TagPayload (Collection<TagInputBean>tags){
        this.tags = tags;
    }

    public TagPayload(TagInputBean tagInput) {
        tags = new ArrayList<>();
        tags.add(tagInput);
    }

    public TagPayload setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public TagPayload setIgnoreRelationships(boolean ignoreRelationships) {
        this.ignoreRelationships = ignoreRelationships;
        return this;
    }

    public TagPayload setTags(Collection<TagInputBean> tags) {
        this.tags = tags;
        return this;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean isIgnoreRelationships() {
        return ignoreRelationships;
    }

    public Collection<TagInputBean> getTags() {
        return tags;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
