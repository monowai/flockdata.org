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

package org.flockdata.engine.track.service;

import java.util.Collection;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;

/**
 * @author mholdsworth
 * @since 14/11/2014
 */
public interface FortressService {
  FortressNode getFortress(Long id);

  FortressUserNode getUser(Long id);

  //    @Cacheable(value = "fortressName", unless = "#result == null")
  FortressNode findByName(Company company, String fortressName) throws NotFoundException;

  FortressNode findByName(String fortressName) throws NotFoundException;

  FortressNode findByCode(Company company, String fortressCode);

//    Fortress findByCode(Company company, String fortressCode);

  FortressUserNode getFortressUser(Company company, String fortressName, String fortressUser) throws NotFoundException;

  FortressUserNode getFortressUser(Fortress fortress, String fortressUser);

  FortressUserNode getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing);

  Collection<FortressResultBean> findFortresses() throws FlockException;

  Collection<FortressResultBean> findFortresses(Company company) throws FlockException;

  void purge(Fortress fortress) throws FlockException;

  FortressNode registerFortress(Company company, FortressInputBean fortressInputBean);

  FortressNode registerFortress(Company company, String fortressName);

  FortressNode registerFortress(Company company, FortressInputBean fib, boolean createIfMissing);

  Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) throws NotFoundException;

  Fortress getFortress(Company company, String fortressName) throws NotFoundException;

  String delete(Company company, String fortressName);

  FortressUserNode createFortressUser(Fortress fortress, ContentInputBean inputBean);

  String getGeoQuery(Entity entity);

  EntityTag.TAG_STRUCTURE getTagStructureFinder(Entity entity);

  Segment getDefaultSegment(Fortress fortress);

  Segment addSegment(Segment segment);

  Collection<Segment> getSegments(Fortress fortress);

  Segment resolveSegment(Company company, FortressInputBean fortress, String segmentName, String timeZone) throws NotFoundException;

  FortressNode updateFortress(CompanyNode company, FortressNode existing, FortressInputBean fortressInputBean);

  FortressNode findInternalFortress(Company company);

  FortressInputBean createDefaultFortressInput();

}
