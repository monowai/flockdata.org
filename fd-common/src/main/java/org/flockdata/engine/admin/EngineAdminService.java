/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.engine.admin;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;

import java.util.concurrent.Future;

/**
 * Created by mike on 25/03/15.
 */
public interface EngineAdminService {
    Future<Long> doReindex(Fortress fortress) throws FlockException;

    Future<Long> doReindex(Fortress fortress, String docType) throws FlockException;

    Future<Boolean> purge(Company company, Fortress fortress) throws FlockException ;
}
