/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
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

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Company getCompany() {
        return this.company;  //To change body of implemented methods use File | Settings | File Templates.
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

    public String getEmail() {
        return email;
    }

    public RegistrationBean setName(final String name) {
        this.name = name;
        return this;
    }

    public RegistrationBean setLogin(final String login) {
        this.login = login;
        return this;
    }

    public RegistrationBean setCompanyName(final String companyName) {
        this.companyName = companyName;
        return this;
    }

    public RegistrationBean setCompany(final Company company) {
        this.company = company;
        if ( company != null )
            this.companyName = company.getName();

        return this;
    }

    public RegistrationBean setUnique(final boolean unique) {
        this.unique = unique;
        return this;
    }

    public RegistrationBean setEmail(final String email) {
        this.email = email;
        return this;
    }
}


