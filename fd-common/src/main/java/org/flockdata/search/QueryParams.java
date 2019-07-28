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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.track.bean.MatrixInputBean;

/**
 * Encapsulated search parameters
 *
 * @author mholdsworth
 * @since 12/04/2014
 */
public class QueryParams implements QueryInterface {
  boolean searchTagsOnly = false;
  private String searchText = "*";
  private String segment;
  private String index;
  private ArrayList<String> fields;
  private String key;
  private String company;
  private String fortress;
  private String[] types;
  private String[] data;
  private Integer size = null;
  private Integer from = null;
  private boolean entityOnly;
  private String code;
  private Map<String, Object> query; // Raw query to pass through to ES
  private Map<String, Object> aggs; // Raw aggs to pass through to ES
  private Map<String, Object> filter; // Raw filter to pass through to ES
  private Map<String, Object> terms = new HashMap<>(); // Raw filter to pass through to ES
  private ArrayList<String> tags;
  private ArrayList<String> relationships = new ArrayList<>();
  private boolean matchAll;

  public QueryParams(String searchText) {
    this.searchText = searchText;
    if (this.searchText.equals("*")) {
      setMatchAll(true);
    }
  }

  public QueryParams(Segment segment) {
    this(segment.getFortress());
    this.segment = segment.getCode();
  }

  public QueryParams(String index, String docType, String code) {
    this.index = index;
    setTypes(docType);
    setCode(code);
  }

  public QueryParams() {
  }


  private QueryParams(Fortress fortress) {
    this();
    setFortress(fortress.getCode());
    setCompany(fortress.getCompany().getCode());
  }

  public QueryParams(Company company, MatrixInputBean input) {
    this.searchText = input.getQueryString();
    this.company = company.getName();
    this.size = input.getSampleSize();
    this.tags = input.getConcepts();
    if (input.getFromRlxs() != null && !input.getFromRlxs().isEmpty()) {
      this.relationships.addAll(input.getFromRlxs());
    }

    if (input.getToRlxs() != null && !input.getToRlxs().isEmpty()) {
      this.relationships.addAll(input.getToRlxs());
    }

    if (input.getDocuments() != null && !input.getDocuments().isEmpty()) {
      types = new String[input.getDocuments().size()];
      int i = 0;
      for (String s : input.getDocuments()) {
        this.types[i++] = s.toLowerCase();
      }
    }
  }

  public QueryParams(String index, String text) {
    this(text);
    this.index = index;
  }

  public ArrayList<String> getTags() {
    return tags;
  }

  public String getSearchText() {
    return searchText;
  }

  public void setSearchText(String searchText) {
    this.searchText = searchText;
  }

  public String getCompany() {
    return company;
  }

  public QueryParams setCompany(String company) {
    this.company = company;
    return this;
  }

  public String getFortress() {
    return fortress;
  }

  public QueryParams setFortress(String fortress) {
    this.fortress = fortress;
    return this;
  }

  public String[] getTypes() {
    return types;
  }

  public QueryParams setTypes(String... types) {
    this.types = types;
    return this;
  }

  public String[] getData() {
    return data;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public Integer getFrom() {
    return from;
  }

  public void setFrom(Integer from) {
    this.from = from;
  }

  @Override
  public String toString() {
    return "QueryParams{" +
        "searchText='" + searchText + '\'' +
        ", company='" + company + '\'' +
        ", fortress='" + fortress + '\'' +
        ", docTypes='" + Arrays.toString(types) + '\'' +
        ", segment='" + segment + '\'' +
        '}';
  }

  public boolean isEntityOnly() {
    return entityOnly;
  }

  public QueryParams setEntityOnly(boolean entityOnly) {
    this.entityOnly = entityOnly;
    return this;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getCode() {
    return code;
  }

  public QueryParams setCode(String code) {
    this.code = code;
    return this;
  }

  public ArrayList<String> getRelationships() {
    return relationships;
  }

  /**
   * @return elasticsearch body to execute against the requested index
   */
  @Deprecated
  public Map<String, Object> getQuery() {
    return query;
  }

  public QueryParams setQuery(Map<String, Object> query) {
    this.query = query;
    return this;
  }

  public Map<String, Object> getAggs() {
    return aggs;
  }

  public ArrayList<String> getFields() {
    return fields;
  }

  public QueryParams setFields(ArrayList<String> fields) {
    this.fields = fields;
    return this;
  }

  public String getSegment() {
    return segment;
  }

  public QueryParams setSegment(String segment) {
    this.segment = segment;
    return this;
  }

  public String getIndex() {
    return index;
  }

  public QueryParams setIndex(String index) {
    this.index = index;
    return this;
  }

  public QueryParams searchTags() {
    this.searchTagsOnly = true;
    return this;
  }

  public boolean isSearchTagsOnly() {
    return searchTagsOnly;
  }

  @Override
  public Map<String, Object> getFilter() {
    return filter;
  }

  public Map<String, Object> getTerms() {
    return terms;
  }

  @Override
  public QueryParams addTerm(String field, Object value) {
    terms.put(field, value);

    return this;
  }

  public boolean isMatchAll() {
    return matchAll;
  }

  public QueryParams setMatchAll(boolean matchAll) {
    this.matchAll = matchAll;
    return this;
  }
}
