/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.Company;

/**
 * User: Mike Holdsworth
 * Date: 14/05/13
 * Time: 5:53 PM
 */
public class RegistrationBean {
    private String name;
    private String login;
    private String companyName;
    private Company company;
    private boolean unique = false;

    public RegistrationBean() {
    }

    public RegistrationBean(String companyName, String login) {
        this.companyName = companyName;
        this.login = login;
    }

    public RegistrationBean(String companyName, String login, String name) {
        this.companyName = companyName;
        this.login = login;
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Company getCompany() {
        return this.company;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public RegistrationBean setIsUnique(boolean mustBeUnique) {
        this.unique = mustBeUnique;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }
}
