package com.auditbucket.search.model;

/**
 * Encapsulated search parameters
 * User: mike
 * Date: 12/04/14
 * Time: 9:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryParams {
    private String simpleQuery;
    private String company;
    private String fortress;
    private String type;


    public String getSimpleQuery() {
        return simpleQuery;
    }

    public QueryParams setSimpleQuery(String simpleQuery) {
        this.simpleQuery = simpleQuery;
        return this;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
