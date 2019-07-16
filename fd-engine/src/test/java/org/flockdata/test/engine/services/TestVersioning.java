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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.Store;
import org.flockdata.test.engine.FdNodeHelper;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Fortress
 *
 * @author mholdsworth
 * @since 22/03/2015
 */
public class TestVersioning extends EngineBase {
    @Test
    public void defaults_Fortress() throws Exception {
        // DAT-346
        SystemUser su = registerSystemUser();
        assertEquals(Boolean.TRUE, engineConfig.storeEnabled());
        // System default behaviour controlled by configuration.properties
        FortressInputBean fib = new FortressInputBean("vtest");
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertTrue(fortress.isStoreEnabled());

        engineConfig.setStoreEnabled(false);
        assertEquals(Boolean.FALSE, engineConfig.storeEnabled());
        fib = new FortressInputBean("disabledTest");
        assertEquals(null, fib.isStoreEnabled());
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertFalse("System default should have been returned", fortress.isStoreEnabled());

        engineConfig.setStoreEnabled(false);
        fib = new FortressInputBean("manualEnableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setStoreEnabled(true);
        assertTrue("Callers setting did not override System default", fortress.isStoreEnabled());

        engineConfig.setStoreEnabled(true);
        fib = new FortressInputBean("manualDisableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setStoreEnabled(false);
        assertFalse("Callers setting did not override System default", fortress.isStoreEnabled());

    }

    @After
    public void resetDefaults() {
        engineConfig.setStoreEnabled(true);
    }

    @Test
    public void kv_Ignored() throws Exception {
        SystemUser su = registerSystemUser("kv_Ignored", "kv_Ignored");
        engineConfig.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("kv_Ignored", true));
        assertFalse("Store is not disabled", engineConfig.storeEnabled());
        EntityInputBean eib = new EntityInputBean(fortress, "kv_Ignored", "kv_Ignored", new DateTime());
        ContentInputBean cib = new ContentInputBean(ContentDataHelper.getRandomMap());
        eib.setContent(cib);
        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), eib);
        assertEquals("Fortress did not have the store Disabled", Boolean.FALSE, trackResult.getEntity().getFortress().isStoreEnabled());
        EntityLog entityLog = entityService.getLastEntityLog(trackResult.getEntity().getId());
        assertNotNull(entityLog);

        assertNotNull(entityLog.getLog());
        assertNotNull(entityLog.getId());
        assertTrue(entityLog.getLog().isMocked());
        assertTrue("Mocked log has an ID set to current system time", entityLog.getLog().getId() > 0);
        assertEquals("Mocked log should have the same ID as the Entity", trackResult.getEntity().getId(), entityLog.getId());


        EntityNode entity = entityService.getEntity(su.getCompany(), trackResult.getEntity().getKey());
        assertNotNull(entity);

        Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), trackResult.getEntity().getKey());
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        // Check various properties that we still want to return
        for (EntityLogResult log : logs) {
            assertEquals(entity.getFortressCreatedTz().getMillis(), log.getWhen().longValue());
            assertNotNull(log.getEvent());
            assertEquals("Create", log.getEvent().getName());
            assertNotNull(log.getWhen());
            assertNotNull(log.getMadeBy());
            assertFalse(log.isVersioned());
        }
        EntityLog mockLog = entityService.getLogForEntity(entity, 0L);
        assertNotNull(mockLog);
        assertNotNull(mockLog.getLog());
        assertTrue(mockLog.isMocked());
        Assert.assertEquals(Store.NONE.name(), mockLog.getLog().getStorage());

        EntitySummaryBean summaryBean = entityService.getEntitySummary(su.getCompany(), entity.getKey());
        assertNotNull(summaryBean);
        assertNotNull(summaryBean.getChanges());
        assertEquals(1, summaryBean.getChanges().size());

        // See TestFdIntegration for a fully integrated version of this test
    }

    @Test
    public void log_ValidateValues() throws Exception {
        Map<String, Object> json = ContentDataHelper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_Disabled");

        FortressInputBean fib = new FortressInputBean("store_Disabled", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean log = new ContentInputBean("store_Disabled", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        org.flockdata.data.EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());
        assertNotNull(entityLog);
        Assert.assertEquals(Store.NONE.name(), entityLog.getLog().getStorage());

//        engineConfig.setStoreEnabled("false");
//        assertEquals("MemMap is OK", storageService.ping());
//        engineConfig.setStoreEnabled("true");

    }

    @Test
    public void storage_CorrectMechanismSelected() throws Exception {
        // DAT-353
        engineConfig.setStoreEnabled(true);
        // The system default store is MEMORY
        ContentInputBean content = new ContentInputBean(ContentDataHelper.getRandomMap());
        // Fortress is not enabled but the overall configuration says the store is enabled
        Entity entity = FdNodeHelper.getEntity("blah", "abc", "abc", "123");

        // set a default for the fortress
        FortressNode fortress = (FortressNode) entity.getFortress();
        fortress.setStoreEnabled(false);
        TrackResultBean trackResult = new TrackResultBean(entity, new DocumentNode("abc"));
        trackResult.setContentInput(content);

        LogNode log = new LogNode(entity);

        log = logRetryService.prepareLog(engineConfig.store(), trackResult, log);
        assertEquals("Store should be set to that of the fortress", Store.NONE.name(), log.getContent().getStore());

        fortress.setStoreEnabled(true);
        log = logRetryService.prepareLog(engineConfig.store(), trackResult, log);
        // Falls back to the system default
        assertEquals("Store should be set to the system default", Store.MEMORY.name(), log.getContent().getStore());


    }

}
