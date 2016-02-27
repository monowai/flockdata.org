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

package org.flockdata.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
@TypeAlias(value = "SystemUser")
public class SystemUser  {
    @GraphId
    Long id;

    private String name = null;

    //@Indexed(unique = true)
    private String login;

    @Indexed
    private String apiKey;

    //@Relationship( type = "ACCESSES", direction = Relationship.OUTGOING)
    @Fetch
    @RelatedTo( type = "ACCESSES", direction = Direction.OUTGOING)
    private Company company;

    protected SystemUser() {
    }

    public SystemUser(String name, String login, org.flockdata.model.Company company, boolean admin) {
        setName(name);
        if ( login == null )
            login = name;
        setLogin(login);

        if (admin)
            setAdministers(company);
    }

    public String getName() {
        return name;
    }

    SystemUser setName(String name) {
        this.name = name;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public SystemUser setLogin(String login) {
        this.login = login.toLowerCase();
        return  this;

    }

    public String getApiKey() {
        return apiKey;
    }

    public SystemUser setApiKey(String openUID) {
        this.apiKey = openUID;
        return this;
    }

    public Company getCompany() {
        return company;
    }

    public Long getId() {
        return id;

    }

    void setAdministers(org.flockdata.model.Company company) {
        this.company = company;
    }

    @Override
    public String toString() {
        return "SystemUserNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", company=" + company +
                '}';
    }
}
