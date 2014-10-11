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

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.DeltaBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.EntityLog;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@Transactional
public class TestDelta extends EngineBase {

    @Test
    public void jsonDeltasAreFound() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("DELTAForce", true));
        assertNotNull(fortress);

        Map<String, Object> jsonA = Helper.getSimpleMap("house", "red");
        jsonA.put("bedrooms", 2);
        jsonA.put("garage", "Y");

        //String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map<String, Object> jsonB = Helper.getSimpleMap("house", "green");
        jsonB.put("bedrooms", 2);
        ArrayList<Integer> values = new ArrayList<>();
        values.add(1);
        values.add(2);
        values.add(3);
        jsonB.put("list", values);

        EntityInputBean entity = new EntityInputBean("DELTAForce", "auditTestz", "Delta", new DateTime(), "abdelta");
        ContentInputBean log = new ContentInputBean("Mike", new DateTime(), jsonA);
        entity.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entity);
        EntityLog first = logService.getLastLog(result.getEntity());
        assertNotNull(first);
        log = new ContentInputBean("Mike", result.getMetaKey(), new DateTime(), jsonB);
        mediationFacade.trackLog(su.getCompany(), log);
        EntityLog second = logService.getLastLog(result.getEntity());
        assertNotNull(second);


        DeltaBean deltaBean = kvService.getDelta(result.getEntity(), first.getLog(), second.getLog());
        Map added = deltaBean.getAdded();
        assertNotNull(added);
        assertTrue(added.containsKey("list"));

        Map removed = deltaBean.getRemoved();
        assertNotNull(removed);
        assertTrue(removed.containsKey("garage"));

        Map changed = deltaBean.getChanged();
        assertNotNull(changed);
        assertTrue(changed.containsKey("house"));

        assertNotNull(deltaBean);


    }


}
