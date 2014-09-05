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

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.AuditDeltaBean;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.TrackLog;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@Transactional
public class TestDelta extends TestEngineBase {

    @Test
    public void jsonDeltasAreFound() throws Exception {
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));

        Fortress fortress = fortressService.registerFortress("DELTAForce");
        assertNotNull(fortress);

        Map<String, Object> jsonA = TestHelper.getSimpleMap("house", "red");
        jsonA.put("bedrooms", 2);
        jsonA.put("garage", "Y");

        //String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map<String, Object> jsonB = TestHelper.getSimpleMap("house", "green");
        jsonB.put("bedrooms", 2);
        ArrayList<Integer> values = new ArrayList<>();
        values.add(1);
        values.add(2);
        values.add(3);
        jsonB.put("list", values);

        MetaInputBean header = new MetaInputBean("DELTAForce", "auditTestz", "Delta", new DateTime(), "abdelta");
        LogInputBean log = new LogInputBean("Mike", new DateTime(), jsonA);
        header.setLog(log);
        TrackResultBean result = mediationFacade.trackHeader(su.getCompany(), header);
        TrackLog first = logService.getLastLog(result.getMetaHeader());
        Assert.assertNotNull(first);
        log = new LogInputBean("Mike", result.getMetaKey(), new DateTime(), jsonB);
        mediationFacade.processLog(su.getCompany(), log);
        TrackLog second = logService.getLastLog(result.getMetaHeader());
        Assert.assertNotNull(second);


        AuditDeltaBean deltaBean = whatService.getDelta(result.getMetaHeader(), first.getLog(), second.getLog());
        Map added = deltaBean.getAdded();
        Assert.assertNotNull(added);
        assertTrue(added.containsKey("list"));

        Map removed = deltaBean.getRemoved();
        Assert.assertNotNull(removed);
        assertTrue(removed.containsKey("garage"));

        Map changed = deltaBean.getChanged();
        Assert.assertNotNull(changed);
        assertTrue(changed.containsKey("house"));

        assertNotNull(deltaBean);


    }


}
