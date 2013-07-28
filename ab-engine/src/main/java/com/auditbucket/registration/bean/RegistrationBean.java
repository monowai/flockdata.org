/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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
import com.auditbucket.registration.model.SystemUser;

/**
 * User: Mike Holdsworth
 * Date: 14/05/13
 * Time: 5:53 PM
 */
public class RegistrationBean implements SystemUser {
    private String name;
    private String password;
    private String companyName;
    private Company company;

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
    public Company getCompany() {
        return this.company;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
