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
import org.flockdata.track.model.KvContent;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Fortress
 * Created by mike on 22/03/15.
 */
public class TestVersioning extends EngineBase {
    @Test
    public void defaults_Fortress() throws Exception {
        // DAT-346
        SystemUser su = registerSystemUser();
        assertEquals(Boolean.TRUE, engineConfig.getVersionEnabled());
        // System default behaviour controlled by configuration.properties
        FortressInputBean fib = new FortressInputBean("vtest");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertTrue(fortress.isVersioningEnabled());

        engineConfig.setVersionEnabled("false");
        assertEquals(Boolean.FALSE, engineConfig.getVersionEnabled());
        fib = new FortressInputBean("disabledTest");
        assertEquals(null, fib.getVersioning());
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertFalse("System default should have been returned", fortress.isVersioningEnabled());

        engineConfig.setVersionEnabled("false");
        fib = new FortressInputBean("manualEnableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setVersioning(true);
        assertTrue("Callers setting did not override System default", fortress.isVersioningEnabled());

        engineConfig.setVersionEnabled("true");
        fib = new FortressInputBean("manualDisableTest");
        fortress = fortressService.registerFortress(su.getCompany(), fib);
        fortress.setVersioning(false);
        assertFalse("Callers setting did not override System default", fortress.isVersioningEnabled());

    }

    @After
    public void resetDefaults() {
        engineConfig.setVersionEnabled("true");
    }

    @Test
    public void kv_Ignored() throws Exception {
        SystemUser su = registerSystemUser("kv_Ignored", "kv_Ignored");
        engineConfig.setVersionEnabled("false");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("kv_Ignored", true));
        assertFalse(engineConfig.getVersionEnabled());
        EntityInputBean eib = new EntityInputBean(fortress.getName(), "kv_Ignored", "kv_Ignored", new DateTime());
        ContentInputBean cib = new ContentInputBean(Helper.getRandomMap());
        eib.setContent(cib);
        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), eib);
        assertEquals(Boolean.FALSE, trackResult.getEntity().getFortress().isVersioningEnabled());
        EntityLog entityLog= entityService.getLastEntityLog(trackResult.getEntity().getId());
        assertNotNull ( entityLog);
        KvContent content  = kvService.getContent(trackResult.getEntity(), entityLog.getLog());
        assertNull(content);

    }
}
