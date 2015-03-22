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

package org.flockdata.test.engine.functional;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.EntityLog;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Fortress
 * Created by mike on 22/03/15.
 */
public class TestVersioning extends EngineBase {
    @Test
    public void defaults_Fortress() throws Exception {
        // DAT-346
        SystemUser su = registerSystemUser();
        assertEquals(Boolean.TRUE, engineConfig.isStoreEnabled());
        // System default behaviour controlled by configuration.properties
        FortressInputBean fib = new FortressInputBean("vtest");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertTrue(fortress.isStoreEnabled());

        engineConfig.setStoreEnabled("false");
        assertEquals(Boolean.FALSE, engineConfig.isStoreEnabled());
        fib = new FortressInputBean("disabledTest");
        assertEquals(null, fib.getStore());
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertFalse("System default should have been returned", fortress.isStoreEnabled());

        engineConfig.setStoreEnabled("false");
        fib = new FortressInputBean("manualEnableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setStoreEnabled(true);
        assertTrue("Callers setting did not override System default", fortress.isStoreEnabled());

        engineConfig.setStoreEnabled("true");
        fib = new FortressInputBean("manualDisableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setStoreEnabled(false);
        assertFalse("Callers setting did not override System default", fortress.isStoreEnabled());

    }

    @After
    public void resetDefaults() {
        engineConfig.setStoreEnabled("true");
    }

    @Test
    public void kv_Ignored() throws Exception {
        SystemUser su = registerSystemUser("kv_Ignored", "kv_Ignored");
        engineConfig.setStoreEnabled("false");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("kv_Ignored", true));
        assertFalse(engineConfig.isStoreEnabled());
        EntityInputBean eib = new EntityInputBean(fortress.getName(), "kv_Ignored", "kv_Ignored", new DateTime());
        ContentInputBean cib = new ContentInputBean(Helper.getRandomMap());
        eib.setContent(cib);
        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), eib);
        assertEquals(Boolean.FALSE, trackResult.getEntity().getFortress().isStoreEnabled());
        EntityLog entityLog= entityService.getLastEntityLog(trackResult.getEntity().getId());
        assertNotNull ( entityLog);
        // DAT-347 - can we mock the call to ES? It's not running here in fd-engine :o)
//        KvContent content  = kvService.getContent(trackResult.getEntity(), entityLog.getLog());
//        assertNotNull(content);
        // This confirms that nothing got saved in the request to write to the kv store.
        // The fact we have a log will mean we also have a KvContent

//        assertNull(content.getAttachment());
//        assertNull(content.getBucket());
//        assertNull(content.getContent());
//        assertNull(content.getWhat());

        // Now for a full integration test
    }
    @Test
    public void log_ValidateValues() throws Exception{
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_Disabled");

        FortressInputBean fib= new FortressInputBean("store_Disabled", true);
        fib.setStore(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean log = new ContentInputBean("store_Disabled", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());
        assertNotNull(entityLog);
        Assert.assertEquals("NONE", entityLog.getLog().getStorage());

        engineConfig.setStoreEnabled("false");
        assertEquals("EsStorage is OK", kvService.ping());
        engineConfig.setStoreEnabled("true");

    }
}
