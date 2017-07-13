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

package org.flockdata.registration;


import org.flockdata.data.Company;

/**
 * @author mholdsworth
 * @since 14/05/2013
 */
public class RegistrationBean {
    private String name;
    private String login;
    private String companyName;
    private Company company;
    private boolean unique = false;
    private String email;

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

    public RegistrationBean(String accessUser) {
        this.login = accessUser;
        this.name  = accessUser;
    }

    public String getCompanyName() {
        return companyName;
    }

    public RegistrationBean setCompanyName(final String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public RegistrationBean setLogin(final String login) {
        this.login = login;
        return this;
    }

    public String getName() {
        return name;
    }

    public RegistrationBean setName(final String name) {
        this.name = name;
        return this;
    }

    public Company getCompany() {
        return this.company;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public RegistrationBean setCompany(final Company company) {
        this.company = company;
        if (company != null)
            this.companyName = company.getName();

        return this;
    }

    public RegistrationBean setIsUnique(boolean mustBeUnique) {
        this.unique = mustBeUnique;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public RegistrationBean setUnique(final boolean unique) {
        this.unique = unique;
        return this;
    }

    @Override
    public String toString() {
        return "RegistrationBean{" +
                "companyName='" + companyName + '\'' +
                ", company=" + company +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getEmail() {
        return email;
    }

    public RegistrationBean setEmail(final String email) {
        this.email = email;
        return this;
    }
}


