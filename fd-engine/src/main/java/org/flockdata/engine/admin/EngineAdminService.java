/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.admin;

import java.util.Collection;
import java.util.concurrent.Future;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.helper.FlockException;

/**
 * @author mholdsworth
 * @since 25/03/2015
 */
public interface EngineAdminService {
  Future<Long> doReindex(Fortress fortress) throws FlockException;

  Future<Long> doReindex(Fortress fortress, String docType) throws FlockException;

  Future<Long> doReindex(Fortress fortress, Entity entity) throws FlockException;

  Future<Boolean> purge(Fortress fortress) throws FlockException;

  /**
   * @param company      who owns the data
   * @param fortress     fortress within the company
   * @param documentType documents in the fortress
   * @param segment      Optional segment. if null, all segments are deleted
   * @return success
   */
  Future<Boolean> purge(Company company, Fortress fortress, Document documentType, String segment);

  Future<Collection<String>> validateFromSearch(Company company, Fortress fortress, String docType) throws FlockException;

  void doReindex(Entity entity) throws FlockException;

}
