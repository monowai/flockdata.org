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

package com.auditbucket.core.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ISystemUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
public class SystemUser implements ISystemUser {
    @GraphId
    Long id;

    @Indexed(unique = true, indexName = "sysUserName")
    private String name = null;

    private String password;

    private String openUID;

//    @RelatedTo (elementClass = CompanyUser.class, type ="isA", direction = Direction.INCOMING)
//    private ICompanyUser companyUser;

    @Fetch
    @RelatedTo(elementClass = Company.class, type = "administers", direction = Direction.OUTGOING)
    private Company company;


    protected SystemUser() {
    }

    public SystemUser(String name, String password, ICompany company, boolean admin) {
        setName(name);
        setPassword(password);

        if (admin)
            setAdministers(company);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOpenUID() {
        return openUID;
    }

    public void setOpenUID(String openUID) {
        this.openUID = openUID;
    }

//    public ICompanyUser getCompanyUser() {
//        return companyUser;
//    }
//
//    public void setCompanyUser(ICompanyUser companyUser) {
//        this.companyUser = companyUser;
//    }

    public ICompany getCompany() {
        return company;
    }

    public void setAdministers(ICompany company) {
        this.company = (Company) company;
    }


}
