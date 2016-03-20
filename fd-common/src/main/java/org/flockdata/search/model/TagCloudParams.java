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

package org.flockdata.search.model;

import org.flockdata.model.Fortress;

import java.util.ArrayList;

/**
 * A POJO that represent a bean that Transit in Spring integration
 * User: Nabil
 * Date: 12/10/2014
 * Time: 17:49
 */
public class TagCloudParams implements QueryInterface {

    private String company;

    // ToDo: Can this be an Array[] ?
    private String fortress = "*";
    // ToDo: This should be an Array[]
    private ArrayList<String> types = new ArrayList<>();

    private ArrayList<String> tags = new ArrayList<>();

    private ArrayList<String> relationships = new ArrayList<>();
    private String searchText;
    private String segment;

    public TagCloudParams() {
    }

    public TagCloudParams(Fortress fortress) {
        this();
        setFortress(fortress.getCode());
        setCompany(fortress.getCompany().getCode());
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    @Override
    public String[] getTypes() {
        String[] results = new String[types.size()];
        int i = 0;
        for (String s : types) {
            results[i] = s.toLowerCase();
            i++;
        }
        return results;
    }

    public void addType(String type) {
        this.types.add(type);
    }

    public void setTypes(ArrayList<String> types) {
        this.types = types;
    }

    @Override
    public ArrayList<String> getRelationships() {
        return relationships;
    }

    public void setRelationships(ArrayList<String> relationships) {
        this.relationships = relationships;
    }

    @Override
    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    @Override
    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String query) {
        this.searchText = query;
    }

    public TagCloudParams addRelationship(String relationship) {
        if ( relationships == null )
            relationships = new ArrayList<>();
        relationships.add(relationship);
        return this;
    }

    public TagCloudParams addTag(String tagLabel) {
        if ( tags == null )
            tags = new ArrayList<>();
        tags.add(tagLabel);
        return this;
    }
}
