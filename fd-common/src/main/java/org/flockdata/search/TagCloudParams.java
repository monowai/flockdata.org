/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.search;

import java.util.ArrayList;
import java.util.Map;

/**
 * A POJO that represent a bean that Transit in Spring integration
 *
 * @author Nabil
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
  public Map<String, Object> getFilter() {
    return null;
  }

  @Override
  public QueryParams addTerm(String field, Object searchText) {
    return null;
  }

  public TagCloudParams addRelationship(String relationship) {
    if (relationships == null) {
      relationships = new ArrayList<>();
    }
    relationships.add(relationship);
    return this;
  }

  public TagCloudParams addTag(String tagLabel) {
    if (tags == null) {
      tags = new ArrayList<>();
    }
    tags.add(tagLabel);
    return this;
  }

}
