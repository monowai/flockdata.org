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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface FortressDao {

    public Fortress save(Fortress fortress);

    public Fortress findByPropertyValue(String name, Object value);

    public Fortress findOne(Long id);

    public FortressUser getFortressUser(Long id, String name);

    List<Fortress> findFortresses(Long companyID);

    FortressUser findOneUser(Long id);

    FortressUser save(Fortress fortress, String fortressUserName);

    void fetch(FortressUser lastUser);
}
