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

package org.flockdata.test.engine.mvc;

import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Entity;
import org.flockdata.model.EntityLog;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.junit.Test;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 29/10/14
 * Time: 11:59 AM
 */
@WebAppConfiguration
public class TestEndPoints extends WacBase {

    @Test
    public void track_MinimalArguments () throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        ContentInputBean cib = new ContentInputBean("userA", EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        Entity e = getEntity(mike(), trackResult.getMetaKey());
        //Entity e = entityService.getEntity(su.getCompany(), trackResult.getMetaKey());

        assertEquals("usera", e.getLastUser().getCode());
        assertEquals("usera", e.getCreatedBy().getCode());

    }

    @Test
    public void track_FortressUserInEntity() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull("FortressUser in the Header, but not in Content, should work", trackResult);
        Entity e = getEntity(mike(), trackResult.getMetaKey());

        assertEquals("usera", e.getLastUser().getCode());
        assertEquals("usera", e.getCreatedBy().getCode());
    }

    @Test
    public void entity_findLogs() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("entity_findLogs", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        Collection<EntityLog> entityLogs = getEntityLogs(mike(), trackResult.getMetaKey());
        assertEquals(1, entityLogs.size());
    }

    @Test
    public void entity_findLogsWithIllegalEntity() throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("entity_findLogsWithIllegalEntity", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        // Test Serialization
        byte[] bytes =JsonUtils.toJsonBytes(eib);
        eib = JsonUtils.toObject(bytes,EntityInputBean.class);

        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        login("mike", "123");
        getEntityLogsIllegalEntity(mike(), trackResult.getMetaKey() +"123");

    }

    @Test
    public void fortress_ResultProperties() throws Exception{
        FortressInputBean fortressInputBean = new FortressInputBean("Twitter");
        FortressResultBean result = makeFortress(mike(), fortressInputBean);
        assertEquals("Twitter", result.getName());
        assertEquals("twitter", result.getCode());

        fortressInputBean = new FortressInputBean("twitter");
        result = makeFortress(mike(), fortressInputBean);
        assertEquals("Twitter", result.getName());
        assertEquals("twitter", result.getCode());
        assertNotNull("Index Name not found", result.getIndexName());

        FortressResultBean fortress = getFortress(mike(), result.getCode());
        assertEquals(fortress.getRootIndex(), result.getIndexName());
        assertEquals("Creation of a fortress should be case insensitive", 1, getFortresses(mike()).size());

    }



}
