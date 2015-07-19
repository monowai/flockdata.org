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

package org.flockdata.test.engine.functional;

import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.FortressResultBean;
import org.flockdata.test.engine.Helper;
import org.flockdata.test.engine.endpoint.EngineEndPoints;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 29/10/14
 * Time: 11:59 AM
 */
@WebAppConfiguration
public class TestEndPoints extends EngineBase{
    @Autowired
    WebApplicationContext wac;
    @Test
    public void track_MinimalArguments () throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("track_MinimalArguments", "userA");
        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        ContentInputBean cib = new ContentInputBean("userA", Helper.getRandomMap());
        eib.setContent(cib);
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        TrackRequestResult trackResult = engineEndPoints.track(eib, su);
        assertNotNull(trackResult);
        Entity e = entityService.getEntity(su.getCompany(), trackResult.getMetaKey());

        assertEquals("usera", e.getLastUser().getCode());
        assertEquals("usera", e.getCreatedBy().getCode());

    }

    @Test
    public void track_FortressUserInEntity() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("track_MinimalArguments", "userA");
        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(Helper.getRandomMap());
        eib.setContent(cib);
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        engineEndPoints.login("mike", "123");
        TrackRequestResult trackResult = engineEndPoints.track(eib, su);
        assertNotNull("FortressUser in the Header, but not in Content, should work", trackResult);
        Entity e = entityService.getEntity(su.getCompany(), trackResult.getMetaKey());

        assertEquals("usera", e.getLastUser().getCode());
        assertEquals("usera", e.getCreatedBy().getCode());
    }

    @Test
    public void fortress_CreationUpdate() throws Exception{
        cleanUpGraph();
        setSecurity();
        SystemUser su = registerSystemUser("fortress_CreationUpdate", "userA");
        setSecurityEmpty();
        FortressInputBean fortressInputBean = new FortressInputBean("Twitter");
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        FortressResultBean result = engineEndPoints.postFortress(su, fortressInputBean);
        assertEquals("Twitter", result.getName());
        assertEquals("twitter", result.getCode());

        fortressInputBean = new FortressInputBean("twitter");
        result = engineEndPoints.postFortress(su, fortressInputBean);
        assertEquals("Twitter", result.getName());
        assertEquals("twitter", result.getCode());
        assertEquals("Creation of a fortress should be case insensitive", 1, fortressService.findFortresses(su.getCompany()).size());

    }
}
