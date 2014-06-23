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

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 10:41 AM
 */
@Transactional
public class TestTxReference extends TestEngineBase{


    private String escJsonA = "{\"blah\":1}";
    private String escJsonB = "{\"blah\":2}";

   

    @Test
    public void testAuthorisedToViewTransaction() throws Exception {
        SystemUser suABC = regService.registerSystemUser(new RegistrationBean("ABC", "mike", "Mike Holdsworth"));
        SystemUser suCBA = regService.registerSystemUser(new RegistrationBean("CBA", "sally", null));

        Authentication authABC = new UsernamePasswordAuthenticationToken(suABC.getLogin(), "123");
        Authentication authCBA = new UsernamePasswordAuthenticationToken(suCBA.getLogin(), "123");

// ABC Data
        Fortress fortressABC = fortressService.registerFortress("abcTest");
        MetaInputBean abcHeader = new MetaInputBean(fortressABC.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        abcHeader.setLog(new LogInputBean(null, "charlie", DateTime.now(), escJsonA, true));

        TrackResultBean resultBean = mediationFacade.createHeader(abcHeader, null);
        LogResultBean logResultBean = resultBean.getLogResult();
        assertNotNull(logResultBean);
        String abcTxRef = logResultBean.getTxReference();
        assertNotNull(abcTxRef);

// CBA data
        SecurityContextHolder.getContext().setAuthentication(authCBA);
        Fortress fortressCBA = fortressService.registerFortress("cbaTest");
        MetaInputBean cbaHeader = new MetaInputBean(fortressCBA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String cbaKey = mediationFacade.createHeader(cbaHeader, null).getMetaKey();

        LogInputBean cbaLog = new LogInputBean(cbaKey, "charlie", DateTime.now(), escJsonA, true);
        assertEquals("CBA Logger Not Created", LogInputBean.LogStatus.OK, mediationFacade.processLog(cbaLog).getLogResult().getStatus());
        String cbaTxRef = cbaLog.getTxRef();
        assertNotNull(cbaTxRef);

        // CBA Caller can not see the ABC transaction
        assertNotNull(trackService.findTx(cbaTxRef));
        assertNull(trackService.findTx(abcTxRef));

        // ABC Caller cannot see the CBA transaction
        SecurityContextHolder.getContext().setAuthentication(authABC);
        assertNotNull(trackService.findTx(abcTxRef));
        assertNull(trackService.findTx(cbaTxRef));

        // WHat happens if ABC tries to use CBA's TX Ref.
        abcHeader = new MetaInputBean(fortressABC.getName(), "wally", "TestTrack", new DateTime(), "ZZZAAA");
        abcHeader.setLog(new LogInputBean(null, "wally", DateTime.now(), escJsonA, null, cbaTxRef));
        TrackResultBean result = mediationFacade.createHeader(abcHeader, null);
        assertNotNull(result);
        // It works because TX References have only to be unique for a company
        //      ab generated references are GUIDs, but the caller is allowed to define their own transaction
        assertNotNull(trackService.findTx(cbaTxRef));


    }

    @Test
    public void testTxCommits() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, mike));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        MetaInputBean aBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");

        String key = mediationFacade.createHeader(aBean, null).getMetaKey();
        assertNotNull(key);
        MetaHeader header = trackService.getHeader(key);
        assertNotNull(header);
        //assertEquals(1, header.getTxTags().size());
        LogInputBean alb = new LogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = mediationFacade.processLog(alb).getLogResult().getTxReference();

        alb = new LogInputBean(key, "harry", DateTime.now(), escJsonB);


        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        mediationFacade.processLog(alb);
        Map<String, Object> result = trackService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Collection<Log> logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        // Create a new Logger for a different transaction
        alb = new LogInputBean(key, "mikey", DateTime.now(), escJsonA);
        alb.setTransactional(true);
        assertNull(alb.getTxRef());
        alb.setTxRef("");
        assertNull("Should be Null if it is blank", alb.getTxRef());
        assertTrue(alb.isTransactional());
        LogResultBean arb = mediationFacade.processLog(alb).getLogResult();
        String txEnd = arb.getTxReference();
        assertNotNull(txEnd);
        assertNotSame(txEnd, txStart);

        result = trackService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        result = trackService.findByTXRef(txEnd);
        assertNotNull(result);
        assertEquals(txEnd, result.get("txRef"));
        logs = (Collection<Log>) result.get("logs");
        assertNotNull(logs);
        assertEquals(1, logs.size());


    }

    @Test
    public void txHeadersTracked() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, mike));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        MetaInputBean aBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");

        String key = mediationFacade.createHeader(aBean, null).getMetaKey();
        assertNotNull(key);
        MetaHeader header = trackService.getHeader(key);
        assertNotNull(header);
        LogInputBean alb = new LogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = mediationFacade.processLog(alb).getLogResult().getTxReference();

        alb = new LogInputBean(key, "harry", DateTime.now(), escJsonB);

        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        mediationFacade.processLog(alb);
        // All headers touched by this transaction. ToDo: All changes affected
        Set<MetaHeader> result = trackService.findTxHeaders(txStart);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        for (MetaHeader metaHeader : result) {
            assertNotNull(metaHeader.getMetaKey());
        }


    }
}
