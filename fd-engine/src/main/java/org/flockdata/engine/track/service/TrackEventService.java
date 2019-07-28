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

import org.flockdata.data.ChangeEvent;
import org.flockdata.data.Company;
import org.flockdata.engine.data.graph.ChangeEventNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 27/06/2013
 */
@Transactional
@Service
public class TrackEventService {


  /**
   * associates the change with the event name for the company. Creates if it does not exist
   *
   * @param eventCode - descriptive name of the event - duplicates for a company will not be created
   * @return created ChangeEvent
   */
  @Transactional(propagation = Propagation.SUPPORTS)
  public ChangeEvent processEvent(String eventCode) {
    //Company company = securityHelper.getCompany();
    return processEvent(null, eventCode);
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public ChangeEvent processEvent(Company company, String eventCode) {
    //return trackEventDao.createEvent(company, eventCode);
    return new ChangeEventNode(eventCode);
  }

}
