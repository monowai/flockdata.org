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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.EntityLog;
import org.flockdata.data.Log;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author mholdsworth
 * @since 15/06/2013
 */
public class TestTxReference extends EngineBase {

    private static Map<String, Object> escJsonA;
    private static Map<String, Object> escJsonB;

    static {
        escJsonA = new HashMap<>();
        escJsonB = new HashMap<>();
        escJsonA.put("blah", 1);
        escJsonB.put("blah", 2);
    }

    @org.junit.Before
    public void testMode() {
        cleanUpGraph();
        engineConfig.setTestMode(true);
    }

    @Test
    public void testAuthorisedToViewTransaction() throws Exception {
        SystemUser suABC = registerSystemUser("ABC", mike_admin);
        SystemUser suCBA = registerSystemUser("CBA", sally_admin);

        Authentication authABC = new UsernamePasswordAuthenticationToken(suABC.getLogin(), "123");
        Authentication authCBA = new UsernamePasswordAuthenticationToken(suCBA.getLogin(), "123");

// ABC Data
        FortressNode fortressABC = fortressService.registerFortress(suABC.getCompany(), new FortressInputBean("abcTest", true));
        EntityInputBean abcEntity = new EntityInputBean(fortressABC, "wally", "TestTrack", new DateTime(), "ABC123");
        abcEntity.setContent(new ContentInputBean("charlie", null, DateTime.now(), escJsonA, true));

        TrackResultBean resultBean = mediationFacade.trackEntity(suABC.getCompany(), abcEntity);
        EntityLog entityLog = resultBean.getCurrentLog();
        assertNotNull(entityLog);
        assertNotNull(resultBean.getTxReference());
        String abcTxRef = resultBean.getTxReference().getName();
        assertNotNull(abcTxRef);

// CBA data
        SecurityContextHolder.getContext().setAuthentication(authCBA);
        FortressNode fortressCBA = fortressService.registerFortress(suCBA.getCompany(), new FortressInputBean("cbaTest", true));
        EntityInputBean cbaEntity = new EntityInputBean(fortressCBA, "wally", "TestTrack", new DateTime(), "ABC123");
        String cbaKey = mediationFacade.trackEntity(suCBA.getCompany(), cbaEntity).getEntity().getKey();

        ContentInputBean cbaContent = new ContentInputBean("charlie", cbaKey, DateTime.now(), escJsonA, true);
        Assert.assertEquals("CBA Log Not Created", ContentInputBean.LogStatus.OK, mediationFacade.trackLog(suCBA.getCompany(), cbaContent).getLogStatus());
        String cbaTxRef = cbaContent.getTxRef();
        assertNotNull(cbaTxRef);

        // CBA Caller can not see the ABC transaction
        assertNotNull(txService.findTx(cbaTxRef));
        assertNull(txService.findTx(abcTxRef));

        // ABC Caller cannot see the CBA transaction
        SecurityContextHolder.getContext().setAuthentication(authABC);
        assertNotNull(txService.findTx(abcTxRef));
        assertNull(txService.findTx(cbaTxRef));

        // WHat happens if ABC tries to use CBA's TX Ref.
        abcEntity = new EntityInputBean(fortressABC, "wally", "TestTrack", new DateTime(), "ZZZAAA");
        abcEntity.setContent(new ContentInputBean("wally", null, DateTime.now(), escJsonA, null, cbaTxRef));
        TrackResultBean result = mediationFacade.trackEntity(suABC.getCompany(), abcEntity);
        assertNotNull(result);
        // It works because TX References have only to be unique for a company
        //      ab generated references are GUIDs, but the caller is allowed to define their own transaction
        assertNotNull(txService.findTx(cbaTxRef));


    }

    @Test
    public void testTxCommits() throws Exception {
        String company = "Monowai";
        SystemUser su = registerSystemUser(company, mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testTxCommits", true));
        String tagRef = "MyTXTag";
        EntityInputBean aBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");

        String key = mediationFacade.trackEntity(su.getCompany(), aBean).getEntity().getKey();
        assertNotNull(key);
        EntityNode entity = entityService.getEntity(su.getCompany(), key);
        assertNotNull(entity);
        //assertEquals(1, entity.getTxTags().size());
        ContentInputBean alb = new ContentInputBean("charlie", key, DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = mediationFacade.trackLog(su.getCompany(), alb).getTxReference().getName();

        alb = new ContentInputBean("harry", key, DateTime.now(), escJsonB);


        alb.setTxRef(albTxRef);

        TrackResultBean resultBean = mediationFacade.trackLog(su.getCompany(), alb);
        Map<String, Object> result = txService.findByTXRef(albTxRef);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Collection<Log> logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        // Create a new Logger for a different transaction
        alb = new ContentInputBean("mikey", key, DateTime.now(), escJsonA);
        alb.setTransactional(true);
        assertNull(alb.getTxRef());
        alb.setTxRef("");
        assertNull("Should be Null if it is blank", alb.getTxRef());
        assertTrue(alb.isTransactional());
        TrackResultBean arb = mediationFacade.trackLog(su.getCompany(), alb);
        String txEnd = arb.getTxReference().getName();
        assertNotNull(txEnd);
        assertNotSame(txEnd, albTxRef);

        result = txService.findByTXRef(albTxRef);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        result = txService.findByTXRef(txEnd);
        assertNotNull(result);
        assertEquals(txEnd, result.get("txRef"));
        logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(1, logs.size());


    }

    @Test
    public void tx_TrackEntities() throws Exception {

        String company = "Monowai";
        SystemUser su = registerSystemUser(company, mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tx_TrackEntities", true));
        String tagRef = "MyTXTag";
        EntityInputBean aBean = new EntityInputBean(fortress, "wally", "TestTrackDoc", new DateTime(), "tx_TrackEntities");

        String key = mediationFacade.trackEntity(su.getCompany(), aBean).getEntity().getKey();
        assertNotNull(key);
        EntityNode entity = entityService.getEntity(su.getCompany(), key);
        assertNotNull(entity);
        ContentInputBean alb = new ContentInputBean("charlie", key, DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = mediationFacade.trackLog(su.getCompany(), alb).getTxReference().getName();

        alb = new ContentInputBean("harry", key, DateTime.now(), escJsonB);

        alb.setTxRef(albTxRef);

        mediationFacade.trackLog(su.getCompany(), alb);
        // All entities touched by this transaction. ToDo: All changes affected
        Set<EntityNode> result = txService.findTxEntities(albTxRef);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        for (EntityNode entityResult : result) {
            assertNotNull(entityResult.getKey());
        }


    }
}
