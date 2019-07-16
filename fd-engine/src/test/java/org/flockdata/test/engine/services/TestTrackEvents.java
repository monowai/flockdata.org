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

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flockdata.data.ChangeEvent;
import org.flockdata.data.Company;
import org.flockdata.data.EntityLog;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;


/**
 * @author mholdsworth
 * @since 6/09/2013
 */
public class TestTrackEvents extends EngineBase {

    @Test
    public void noDuplicateEventsForACompany() throws Exception {
        SystemUser su = registerSystemUser("noDuplicateEventsForACompany", mike_admin);
        FortressNode fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest"));
        Company company = su.getCompany();
        assertNotNull(company);
        String eventName = "DuplicateNotAllowed";
        ChangeEvent event = trackEventService.processEvent(eventName);

        assertNotNull(event);
        Long existingId = event.getId();
        assertEquals(eventName, event.getName());
        // DAT-344 - this is now a property not a node so removing this
//        assertEquals(eventName.toLowerCase(), event.getCode());
//        //assertEquals(company.getId(), event.getCompany().getId());
//        Set<ChangeEvent> events = trackEventService.getCompanyEvents(company.getId());
//        assertEquals(1, events.size());
//        event = trackEventService.processEvent(eventName);
//        assertEquals(existingId, event.getId());
//        assertEquals(1, events.size());

    }

    /**
     * Ensures that the event type gets set to the correct default for create and update.
     */
    @Test
    public void defaultEventTypesAreHandled() throws Exception {

        SystemUser su = registerSystemUser("defaultEventTypesAreHandled", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("defaultEventTypes", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String key = resultBean.getEntity().getKey();
        assertNotNull(key);

        EntityNode entity = entityService.getEntity(su.getCompany(), key);
        assertNotNull(entity.getType());

        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", key, new DateTime(), ContentDataHelper.getRandomMap()));

        EntityLog when = entityService.getLastEntityLog(su.getCompany(), key);
        assertNotNull(when);
        assertEquals(LogNode.CREATE, when.getLog().getEvent().getName()); // log event default
        assertEquals(LogNode.CREATE.toLowerCase(), when.getLog().getEvent().getName().toLowerCase()); // log event default

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", key, new DateTime(), ContentDataHelper.getRandomMap()));
        EntityLog whenB = entityService.getLastEntityLog(su.getCompany(), key);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertNotNull(whenB.getLog().getEvent());
        assertEquals(LogNode.UPDATE, whenB.getLog().getEvent().getName());  // log event default
    }
}