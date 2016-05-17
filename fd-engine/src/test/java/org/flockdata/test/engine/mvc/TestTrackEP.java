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
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.*;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 29/10/14
 * Time: 11:59 AM
 */
public class TestTrackEP extends MvcBase {

    @Test
    public void track_MinimalArguments () throws Exception {
        FortressResultBean f = makeFortress(mike(),  new FortressInputBean("track_MinimalArguments", true));
        EntityInputBean eib = new EntityInputBean(f.getName(), "DocType");
        eib.setFortressUser("usera");
        eib.setCode(new DateTime().toString());
        ContentInputBean cib = new ContentInputBean("userA", EntityContentHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        assertEquals("A new entity should have been created", true, trackResult.isNewEntity());
        assertEquals( "No service message was returned", 1, trackResult.getServiceMessages().size());
        EntityBean e = getEntity(mike(), trackResult.getKey());
        //Entity e = entityService.getEntity(su.getCompany(), trackResult.getKey());

        assertEquals("usera", e.getLastUser());
        assertEquals("usera", e.getCreatedUser());
        assertNotNull(e.getSearchKey());

        trackResult =track(mike(), eib);
        assertEquals("Existing entity should have been found", true, !trackResult.isNewEntity());

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
        EntityBean e = getEntity(mike(), trackResult.getKey());

        assertEquals("usera", e.getLastUser());
        assertEquals("usera", e.getCreatedUser());
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
        Collection<EntityLogResult> entityLogs = getEntityLogs(mike(), trackResult.getKey());
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
        getEntityLogsIllegalEntity(mike(), trackResult.getKey() +"123");

    }

    @Test
    public void entity_notFoundException() throws Exception {
        login("mike", "123");
        exception.expect(NotFoundException.class);
        getEntity(mike(), "invalidKey)", MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    public void fortress_DuplicateNameWithProperties() throws Exception{
        String fName= "fortress_DuplicateNameWithProperties";
//        cleanUpGraph();
        FortressInputBean fortressInputBean = new FortressInputBean(fName);
        FortressResultBean result = makeFortress(sally(), fortressInputBean);
        assertEquals(fName, result.getName());
        assertEquals(fName.toLowerCase(), result.getCode());

        // Create the same fortress with a lowercase name.
        fortressInputBean = new FortressInputBean(fName.toLowerCase());
        result = makeFortress(sally(), fortressInputBean);
        assertEquals(fName, result.getName());
        assertEquals(fName.toLowerCase(), result.getCode());
        assertNotNull("Index Name not found", result.getIndexName());

        FortressResultBean fortress = getFortress(sally(), result.getCode());
        assertEquals(fortress.getIndexName(), result.getIndexName());
        assertEquals("Creation of a fortress should be case insensitive", 1, getFortresses(sally()).size());

    }



}
