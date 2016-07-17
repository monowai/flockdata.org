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

import org.flockdata.model.SystemUser;

import java.util.Arrays;

/**
 * User: mike
 * Date: 30/04/14
 * Time: 2:53 PM
 */
public class SystemUserResultBean {

    private String login;
    private String name;
    private String companyName;
    private String apiKey;
    private String userEmail;
    private String status;
    private Object[] userRoles;

    public SystemUserResultBean() {
    }

    public SystemUserResultBean(SystemUser su) {
        this();
        if (su != null) {
            this.apiKey = su.getApiKey();
            this.name = su.getName();
            this.login = su.getLogin();
            if (this.name == null)
                this.name = login;
            if (su.getCompany() != null) // an unauthenticated user does not have a company
                this.companyName = su.getCompany().getName();
        }

    }

    public SystemUserResultBean(SystemUser sysUser, UserProfile userProfile) {
        this(sysUser);
        this.userRoles = userProfile.getUserRoles();
        this.status = userProfile.getStatus();
        this.userEmail = userProfile.getUserEmail();

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

    public String getUserEmail() {
        return userEmail;
    }

    public String getStatus() {
        return status;
    }

    public Object[] getUserRoles() {
        return userRoles;
    }

    @Override
    public String toString() {
        return "SystemUser{" +
                "login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", status='" + status + '\'' +
                ", userRoles=" + Arrays.toString(userRoles) +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
