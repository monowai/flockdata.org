package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ISystemUser;

/**
 * User: mike
 * Date: 14/05/13
 * Time: 5:53 PM
 */
public class RegistrationBean implements ISystemUser {
    private String name;
    private String password;
    private String companyName;
    private ICompany company;

    public RegistrationBean() {
    }

    public RegistrationBean(String companyName, String userName, String password) {
        this.companyName = companyName;
        this.name = userName;
        this.password = password;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    @Override
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ICompany getCompany() {
        return this.company;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCompany(ICompany company) {
        this.company = company;
    }
}
