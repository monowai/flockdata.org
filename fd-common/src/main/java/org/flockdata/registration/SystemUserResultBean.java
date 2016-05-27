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
