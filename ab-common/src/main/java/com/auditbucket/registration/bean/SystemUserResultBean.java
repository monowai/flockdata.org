package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.SystemUser;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 2:53 PM
 */
public class SystemUserResultBean {
    private String companyName ;
    private String apiKey;
    private String name;

    public SystemUserResultBean(){}
    public SystemUserResultBean(SystemUser su) {
        this();
        this.companyName = su.getCompany().getName();
        this.apiKey = su.getUid();
        this.name = su.getName();

    }

    public String getCompanyName() {
        return companyName;
    }

    public String getName() {
        return name;
    }

    public String getApiKey() {
        return apiKey;
    }
}
