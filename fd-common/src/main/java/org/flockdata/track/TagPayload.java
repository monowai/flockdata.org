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

package org.flockdata.track;

import org.flockdata.model.Company;
import org.flockdata.registration.TagInputBean;

import java.util.Collection;

/**
 * Created by mike on 19/06/15.
 */
public class TagPayload {

    String tenant = "";
    boolean ignoreRelationships;
    Collection<TagInputBean> tags;
    Company company;

    TagPayload() {
    }

    public TagPayload(Company company) {
        this();
        this.company = company;
    }

    public TagPayload(Collection<TagInputBean> tags) {
        this();
        this.tags = tags;
    }

    public TagPayload setTenant(String tenant) {
        this.tenant = tenant;
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

    public String getTenant() {
        return tenant;
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
