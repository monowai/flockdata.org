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

package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
@TypeAlias(value = "SystemUser")
public class SystemUserNode implements SystemUser {
    @GraphId
    Long id;

    @Indexed(unique = true)
    private String name = null;

    private String password;

    @Indexed
    private String apiKey;

//    @RelatedTo (elementClass = CompanyUserNode.class, type ="isA", direction = Direction.INCOMING)
//    private CompanyUser companyUser;

    @Fetch
    @RelatedTo(elementClass = CompanyNode.class, type = "ADMINISTERS", direction = Direction.OUTGOING)
    private CompanyNode company;


    protected SystemUserNode() {
    }

    public SystemUserNode(String name, String password, Company company, boolean admin) {
        setName(name);
        setPassword(password);

        if (admin)
            setAdministers(company);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name.toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String openUID) {
        this.apiKey = openUID;
    }

    public Company getCompany() {
        return company;
    }

    @Override
    public Long getId() {
        return id;

    }

    void setAdministers(Company company) {
        this.company = (CompanyNode) company;
    }


}
