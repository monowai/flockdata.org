package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 2:53 PM
 */
public class SystemUserResultBean {
    private String apiKey;
    private String name;
    private String login;
    private Company company;

    public SystemUserResultBean(){}
    public SystemUserResultBean(SystemUser su) {
        this();
        this.company = su.getCompany();
        this.apiKey = su.getApiKey();
        this.name = su.getName();
        this.login = su.getLogin();

    }

    public String getCompanyName() {
        return company.getName();
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getLogin() {
        return login;
    }
}
