/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.search.service.AbSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:35 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestAuditSearch {

    @Autowired
    AbSearchService searchService;


    ObjectMapper om = new ObjectMapper();

    private Logger log = LoggerFactory.getLogger(TestAuditSearch.class);

    //    @Rollback(false)
//    @BeforeTransaction
//    public void cleanUpGraph() {
//        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
//        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
//        SecurityContextHolder.getContext().setAuthentication(authA);
//        Neo4jHelper.cleanDb(template);
//    }
//
    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");

    public void testSearchKeysForNonAccumulatingFortresses() throws Exception {
//        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
//        IFortress fo = fortressService.registerFortress(new FortressInputBean("testSearchCancel", false));
//
//        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
//        inputBean.setAuditLog(new AuditLogInputBean("wally", new DateTime(), "{\"blah\":" + 0 + "}"));
//        String ahKey = auditService.createHeader(inputBean).getAuditKey();
//
//        assertNotNull(ahKey);
//        IAuditHeader auditHeader = auditService.getHeader(ahKey);
//        assertNotNull(auditService.getHeader(ahKey));
//        assertNotNull(auditHeader.getSearchKey());
//
//        int i = 1;
//        int max = 10;
//        while (i < max) {
//            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
//            i++;
//        }
//        Set<IAuditLog> logs = auditService.getAuditLogs(ahKey);
//        Iterator<IAuditLog> it = logs.iterator();
//        assertNotNull(logs);
//        assertEquals(max, logs.size());
//        while (it.hasNext()) {
//            IAuditLog next = it.next();
//            assertNull(next.getSearchKey());
//        }
//        byte[] parent = searchService.findOne(auditHeader, auditHeader.getSearchKey());
//
//        assertNotNull(parent);
//        Map<String, Object> ac = om.readValue(parent, Map.class);
//        assertNotNull(ac);
//        assertEquals(auditHeader.getAuditKey(), ac.get("auditKey"));
//        assertEquals("wally", ac.get("who"));
//        assertEquals(max - 1, ac.get("blah"));
//
//        // Test that we synchronise correctly when cancelling
//        i = max - 1;
//        while (i > 0) {
//            auditService.cancelLastLog(ahKey);
//            parent = searchService.findOne(auditHeader);
//            ac = om.readValue(parent, Map.class);
//            assertEquals(i - 1, ac.get("blah"));
//            i--;
//        }

    }
}
