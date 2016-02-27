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

    @Override
    public String toString() {
        return "SystemUserResultBean{" +
                "login='" + login + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
