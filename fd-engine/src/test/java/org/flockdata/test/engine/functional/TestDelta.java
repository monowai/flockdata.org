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

import org.flockdata.model.EntityLog;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
public class TestDelta extends EngineBase {

    @Test
    public void jsonDeltasAreFound() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("DELTAForce", true));
        assertNotNull(fortress);

        Map<String, Object> jsonA = EntityContentHelper.getSimpleMap("house", "red");
        jsonA.put("bedrooms", 2);
        jsonA.put("garage", "Y");

        //String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map<String, Object> jsonB = EntityContentHelper.getSimpleMap("house", "green");
        jsonB.put("bedrooms", 2);
        ArrayList<Integer> values = new ArrayList<>();
        values.add(1);
        values.add(2);
        values.add(3);
        jsonB.put("list", values);

        EntityInputBean entity = new EntityInputBean(fortress, "auditTestz", "Delta", new DateTime(), "abdelta");
        ContentInputBean log = new ContentInputBean("Mike", new DateTime(), jsonA);
        entity.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entity);
        EntityLog first = logService.getLastLog(result.getEntity());
        assertNotNull(first);
        log = new ContentInputBean("Mike", result.getEntity().getMetaKey(), new DateTime(), jsonB);
        mediationFacade.trackLog(su.getCompany(), log);
        EntityLog second = logService.getLastLog(result.getEntity());
        assertNotNull(second);

        //ToDo: fd-store - fix me
//        DeltaResultBean deltaResultBean = kvService.getDelta(result.getEntity(), first.getLog(), second.getLog());
//        Map added = deltaResultBean.getAdded();
//        assertNotNull(added);
//        assertTrue(added.containsKey("list"));
//
//        Map removed = deltaResultBean.getRemoved();
//        assertNotNull(removed);
//        assertTrue(removed.containsKey("garage"));
//
//        Map changed = deltaResultBean.getChanged();
//        assertNotNull(changed);
//        assertTrue(changed.containsKey("house"));
//
//        assertNotNull(deltaResultBean);


    }


}
