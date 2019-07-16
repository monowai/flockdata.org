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

package org.flockdata.test.engine.mvc;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Document;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.track.bean.TrackRequestResult;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @tag Test, Track, MVC
 * @since 29/10/2014
 */
public class TestEntityEP extends MvcBase {

    @Test
    public void find_EntityData() throws Exception {
        FortressResultBean f = makeFortress(mike(), new FortressInputBean("find_EntityData")
            .setSearchEnabled(false)
            .setStoreEnabled(false));
        EntityInputBean eib = new EntityInputBean(f, new DocumentTypeInputBean("find_EntityData")
            .setVersionStrategy(Document.VERSION.ENABLE))
            .setCode("XXX");
        eib.setFortressUser("userA");
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        ContentInputBean cib = new ContentInputBean(data);
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(trackResult);
        Map<String, Object> results = getEntityData(mike(), trackResult.getKey());
        assertNotNull(results);
        assertEquals(data.size(), results.size());
        assertEquals(data.get("key"), results.get("key"));

        results = getEntityData(mike(), eib);
        assertNotNull(results);
        assertEquals(data.size(), results.size());
        assertEquals(data.get("key"), results.get("key"));

    }

    @Test
    public void entity_Summary() throws Exception {
        FortressResultBean f = makeFortress(mike(), new FortressInputBean("entity_Summary", true));
        EntityInputBean eib = new EntityInputBean(f, new DocumentTypeInputBean("Summary"));
        eib.setFortressUser("userA");
        eib.addTag(new TagInputBean("anyTag", "MyLabel", new EntityTagRelationshipInput("twaddle")));
        ContentInputBean cib = new ContentInputBean(ContentDataHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        assertNotNull(getEntity(mike(), trackResult.getKey(), MockMvcResultMatchers.status().isOk()));
        assertNotNull(getEntitySummary(mike(), trackResult.getKey(), MockMvcResultMatchers.status().isOk()));

    }

    @Test
    public void mocked_EntityLogs() throws Exception {
        FortressResultBean f = makeFortress(mike(),
            new FortressInputBean("mocked_EntityLogs")
                .setStoreEnabled(false).setSearchEnabled(false));

        EntityInputBean eib = new EntityInputBean(f, new DocumentTypeInputBean("mocked_EntityLogs"));
        eib.setFortressUser("userA");
        ContentInputBean cib = new ContentInputBean(ContentDataHelper.getRandomMap());
        eib.setContent(cib);
        TrackRequestResult trackResult = track(mike(), eib);
        Collection<EntityLogResult> logs = getEntityLogs(mike(), trackResult.getKey());
        assertEquals("Expected one log", 1, logs.size());
        EntityLogResult entityLog = logs.iterator().next();

        assertNull("The log is not serializable in this view", entityLog.getLog());
        assertTrue(entityLog.isMocked());

    }

    @Test
    public void new_EntityIdentified() throws Exception {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("value", "alpha");

        EntityInputBean entityInputBean = new EntityInputBean()
            .setCode("suppressVersionsOnByDocBasis")
            .setFortress(new FortressInputBean("suppressVersionsOnByDocBasis")
                .setSearchEnabled(false)
                .setStoreEnabled(true)) // Enable the store
            .setDocumentType(new DocumentTypeInputBean("someThing")
                .setVersionStrategy(Document.VERSION.DISABLE)) // But suppress version history for this class of Entity
            .setContent(new ContentInputBean(dataMap));

        TrackRequestResult result = track(mike(), entityInputBean);
        assertEquals(true, result.isNewEntity());

        dataMap.put("value", "beta");

        entityInputBean = new EntityInputBean()
            .setCode("suppressVersionsOnByDocBasis")
            .setFortress(new FortressInputBean("suppressVersionsOnByDocBasis")
                .setSearchEnabled(false)
                .setStoreEnabled(true)) // Enable the store
            .setDocumentType(new DocumentTypeInputBean("someThing")
                .setVersionStrategy(Document.VERSION.DISABLE)) // But suppress version history for this class of Entity
            .setContent(new ContentInputBean(dataMap));

        result = track(mike(), entityInputBean);

        assertEquals("Entity with ame code already exists", false, result.isNewEntity());

    }

}
