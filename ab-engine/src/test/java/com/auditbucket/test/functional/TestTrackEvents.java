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

package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.ChangeEvent;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@Transactional
public class TestTrackEvents extends TestEngineBase{

    @Test
    public void noDuplicateEventsForACompany() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        Company company = fortressA.getCompany();
        assertNotNull(company);
        String eventName = "DuplicateNotAllowed";
        ChangeEvent event = trackEventService.processEvent(eventName);

        assertNotNull(event);
        Long existingId = event.getId();
        assertEquals(eventName, event.getName());
        assertEquals(eventName.toLowerCase(), event.getCode());
        //assertEquals(company.getId(), event.getCompany().getId());
        Set<ChangeEvent> events = trackEventService.getCompanyEvents(company.getId());
        assertEquals(1, events.size());
        event = trackEventService.processEvent(eventName);
        assertEquals(existingId, event.getId());
        assertEquals(1, events.size());

    }

    /**
     * Ensures that the event type gets set to the correct default for create and update.
     */
    @Test
    public void defaultEventTypesAreHandled() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();
        assertNotNull(ahKey);

        MetaHeader header = trackService.getHeader(ahKey);
        assertNotNull(header.getDocumentType());

        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));

        TrackLog when = trackService.getLastLog(ahKey);
        assertNotNull(when);
        assertEquals(Log.CREATE, when.getLog().getEvent().getName()); // log event default
        assertEquals(Log.CREATE.toLowerCase(), when.getLog().getEvent().getName().toLowerCase()); // log event default

        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));
        TrackLog whenB = trackService.getLastLog(ahKey);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertNotNull(whenB.getLog().getEvent());
        assertEquals(Log.UPDATE, whenB.getLog().getEvent().getName());  // log event default
    }
}