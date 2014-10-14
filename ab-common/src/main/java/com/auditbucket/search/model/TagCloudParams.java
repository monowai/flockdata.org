package com.auditbucket.search.model;

import com.auditbucket.registration.model.Fortress;

/**
 * A POJO that represent a bean that Transit in Spring integration
 * User: Nabil
 * Date: 12/10/2014
 * Time: 17:49
 */
public class TagCloudParams {

    private String company;

    // ToDo: Can this be an Array[] ?
    private String fortress;
    // ToDo: This should be an Array[]
    private String type;

    public TagCloudParams() {}
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

    public String getType() {
        return type.toLowerCase();
    }

    public void setType(String type) {
        this.type = type;
    }
}
