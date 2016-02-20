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

package org.flockdata.engine.track.service;

import org.flockdata.dao.TrackEventDao;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.model.ChangeEvent;
import org.flockdata.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Transactional
@Service
public class TrackEventService {

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TrackEventDao trackEventDao;

    /**
     * associates the change with the event name for the company. Creates if it does not exist
     *
     * @param eventCode - descriptive name of the event - duplicates for a company will not be created
     * @return created ChangeEvent
     */
    @Transactional(propagation =  Propagation.SUPPORTS)
    public org.flockdata.model.ChangeEvent processEvent(String eventCode) {
        //Company company = securityHelper.getCompany();
        return processEvent(null, eventCode);
    }

    @Transactional(propagation =  Propagation.SUPPORTS)
    public org.flockdata.model.ChangeEvent processEvent(Company company, String eventCode) {
        //return trackEventDao.createEvent(company, eventCode);
        return new ChangeEvent(eventCode);
    }

    public Set<org.flockdata.model.ChangeEvent> getCompanyEvents(Long id) {
        return trackEventDao.findCompanyEvents(id);
    }
}
