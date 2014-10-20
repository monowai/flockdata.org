/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.registration.bean;

import org.flockdata.registration.model.SystemUser;

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
