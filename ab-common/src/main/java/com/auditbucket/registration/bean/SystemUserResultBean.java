package com.auditbucket.registration.bean;

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
    private String companyName ;

    public SystemUserResultBean(){}
    public SystemUserResultBean(SystemUser su) {
        this();
        this.apiKey = su.getApiKey();
        this.name = su.getName();
        this.login = su.getLogin();
        if (this.name == null )
            this.name = login;
        if ( su.getCompany() !=null ) // an unauthenticated user does not have a company
            this.companyName = su.getCompany().getName();

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

    public String getCompanyName() {
        return companyName;
    }
}
