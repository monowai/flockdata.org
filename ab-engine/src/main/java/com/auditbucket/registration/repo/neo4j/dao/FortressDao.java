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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface FortressDao {

    /**
     * create a fortress
     *
     *
     *
     * @param company
     * @param fortress data to save
     * @return saved fortress
     */
    public Fortress save(Company company, FortressInputBean fortress);

    public Fortress findByPropertyValue(String property, Object value);

    /**
     * Find by primary key
     * @param fortressId  unique identifier
     * @return null if doesn't exist
     */
    public Fortress findOne(Long fortressId);

    public FortressUser getFortressUser(Long fortressId, String name);

    List<Fortress> findFortresses(Long companyID);

    /**
     * Locate a unique fortress user by primary key
     *
     * @param fortressUserId  unique key
     * @return null if doesn't exist
     */
    FortressUser findOneUser(Long fortressUserId);

    /**
     * Associations supplied fortressUserName with the resolved fortress
     *
     * @param fortress resolved unique fortress
     * @param fortressUserName unique username for the fortress
     * @return FortressUser created
     */
    FortressUser save(Fortress fortress, String fortressUserName);

    /**
     * Instantiate lazy loaded properties if appropriate
     *
     * @param fortressUser valid object with an Id
     */
    void fetch(FortressUser fortressUser);

    void delete(Fortress fortress);
}
