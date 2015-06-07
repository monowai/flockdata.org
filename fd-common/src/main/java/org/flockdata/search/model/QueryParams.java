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

package org.flockdata.search.model;

import org.flockdata.query.MatrixInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;

import java.util.ArrayList;
import java.util.Map;

/**
 * Encapsulated search parameters
 * User: mike
 * Date: 12/04/14
 * Time: 9:44 AM
 */
public class QueryParams implements QueryInterface{
    public QueryParams(String searchText) {
        this.searchText = searchText;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    private ArrayList<String> tags;
    private ArrayList<String> relationships = new ArrayList<>();
    private String searchText;

    private Map<String,Object> query; // Raw query to pass through to ES

    private Map<String,Object> aggs; // Raw aggs to pass through to ES

    private ArrayList<String> fields;

    private String metaKey;
    private String company;
    private String fortress;
    private String[] types;
    private String[] data;
    private Integer size =null;
    private Integer from = null;

    private boolean entityOnly;
    private String callerRef;

    public QueryParams() {}

    public QueryParams(Fortress fortress) {
        this();
        setFortress(fortress.getCode());
        setCompany(fortress.getCompany().getCode());
    }

    public QueryParams(Company company, MatrixInputBean input) {
        this.searchText = input.getQueryString();
        this.company = company.getName();
        this.size = input.getSampleSize();
        this.tags = input.getConcepts();
        if ( input.getFromRlxs()!=null && !input.getFromRlxs().isEmpty())
            this.relationships.addAll(input.getFromRlxs());

        if ( input.getToRlxs()!=null && !input.getToRlxs().isEmpty())
            this.relationships.addAll(input.getToRlxs());

        if ( input.getDocuments()!=null && !input.getDocuments().isEmpty()) {
            types = new String[input.getDocuments().size()];
            int i = 0;
            for (String s : input.getDocuments()) {
                this.types[i++]= s.toLowerCase();
            }
        }
    }

    public String getSearchText() {
        return searchText;
    }

    public QueryParams setSearchText(String searchText) {
        this.searchText = searchText;
        return this;
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

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    public String[] getTypes() {
        return types;
    }

    public String[] getData(){
        return data;
    }

    public void setTypes(String... types) {
        this.types = types;
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
                '}';
    }

    public void setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
    }

    public boolean isEntityOnly() {
        return entityOnly;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }


    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    public String getCallerRef() {
        return callerRef;
    }

    public ArrayList<String> getRelationships() {
        return relationships;
    }

    /**
     *
     * @return elasticsearch body to execute against the requested index
     */
    public Map<String,Object> getQuery() {
        return query;
    }

    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }

    public Map<String, Object> getAggs() {
        return aggs;
    }

    public ArrayList<String> getFields() {
        return fields;
    }

}
