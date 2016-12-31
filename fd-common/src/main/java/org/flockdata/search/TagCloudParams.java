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

package org.flockdata.search;

import java.util.ArrayList;
import java.util.Map;

/**
 * A POJO that represent a bean that Transit in Spring integration
 * @author  Nabil
 * @since 12/10/2014
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

    public void setTypes(ArrayList<String> types) {
        this.types = types;
    }

    public void addType(String type) {
        this.types.add(type);
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

    @Override
    public boolean isSearchTagsOnly() {
        return false;
    }

    @Override
    public Map<String,Object> getFilter() {
        return null;
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
