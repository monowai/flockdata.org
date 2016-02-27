/*
 *  Copyright 2012-2016 the original author or authors.
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


import org.flockdata.model.Company;

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
  //TODO: This was changed from default value of true to false
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

    public RegistrationBean(String accessUser) {
        this.login = accessUser;
        this.name  = accessUser;
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
        if ( company != null )
            this.companyName = company.getName();

    }

    public RegistrationBean setIsUnique(boolean mustBeUnique) {
        this.unique = mustBeUnique;
        return this;
    }

    public boolean isUnique() {
        return unique;
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
}
