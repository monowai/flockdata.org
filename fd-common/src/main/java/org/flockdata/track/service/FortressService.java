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

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.track.bean.DocumentResultBean;

import java.util.Collection;

/**
 * User: mike
 * Date: 14/11/14
 * Time: 12:21 PM
 */
public interface FortressService {
    Fortress getFortress(Long id);

    FortressUser getUser(Long id);

    //    @Cacheable(value = "fortressName", unless = "#result == null")
    Fortress findByName(Company company, String fortressName) throws NotFoundException;

    Fortress findByName(String fortressName) throws NotFoundException;

    Fortress findByCode(String fortressCode);

    Fortress findByCode(Company company, String fortressCode);

    FortressUser getFortressUser(Company company, String fortressName, String fortressUser) throws NotFoundException;

    FortressUser getFortressUser(Fortress fortress, String fortressUser);

    FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing);

    Collection<Fortress> findFortresses() throws FlockException;

    Collection<Fortress> findFortresses(Company company) throws FlockException;

    void fetch(FortressUser lastUser);

    void purge(Fortress fortress) throws FlockException;

    Fortress registerFortress(Company company, FortressInputBean fortressInputBean);

    Fortress registerFortress(Company company, FortressInputBean fib, boolean createIfMissing);

    Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) throws NotFoundException;

    Fortress getFortress(Company company, String fortressName) throws NotFoundException;
}
