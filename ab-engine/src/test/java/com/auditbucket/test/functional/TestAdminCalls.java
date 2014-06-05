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

import com.auditbucket.engine.endpoint.AdminEP;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 19/05/14
 * Time: 3:46 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAdminCalls {

    @Autowired
    TrackService trackService;

    @Autowired
    TrackEP trackEP;

    @Autowired
    RegistrationEP regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    AdminEP adminEP;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private MediationFacade mediationFacade;

    private Logger logger = LoggerFactory.getLogger(TestTrack.class);
    private String monowai = "Monowai";
    private String mike = "mike";
    private String mark = "mark";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "123");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "123");

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authMike);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"rest".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Test
    public void deleteFortressWithHeadersAndTagsOnly() throws Exception {

        SystemUserResultBean su = regService.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.setTag(tagInputBean);


        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        inputBean.setTag(tagInputBean);

        mediationFacade.createHeader(inputBean, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        SecurityContextHolder.getContext().setAuthentication(authMike);
        adminEP.purgeFortress(fo.getName(),null, null);
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesHeaderAndLogs() throws Exception {

        SystemUserResultBean su = regService.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));

        assertEquals(2, trackService.getLogCount(resultBean.getMetaKey()));

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        SecurityContextHolder.getContext().setAuthentication(authMike);
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesDataWithTags() throws Exception {

        SystemUserResultBean su = regService.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.setTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));

        inputBean.setCallerRef("123abc");
        inputBean.setLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        mediationFacade.createHeader(inputBean, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail ("An authorisation exception should have been thrown");
        } catch ( Exception e ){
            // This is good
        }
        SecurityContextHolder.getContext().setAuthentication(authMike);
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull( trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));


    }
    @Test
    public void purgeFortressClearsDown() throws Exception{
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUserResultBean su = regService.registerSystemUser(new RegistrationBean(monowai, mike)).getBody();
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("purgeFortressClearsDown", true));

        MetaInputBean trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.setTag( new TagInputBean("anyName", "rlx"));
        trackBean.setTag( new TagInputBean("otherName", "rlxValue").setReverse(true));
        LogInputBean logBean = new LogInputBean("me", DateTime.now(), json );
        trackBean.setLog(logBean);
        String resultA = mediationFacade.createHeader(trackBean, null).getMetaKey();

        assertNotNull(resultA);

        trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.setTag( new TagInputBean("anyName", "rlx"));
        trackBean.setTag( new TagInputBean("otherName", "rlxValue").setReverse(true));
        logBean = new LogInputBean("me", DateTime.now(), json );
        trackBean.setLog(logBean);

        String resultB = mediationFacade.createHeader(trackBean, su.getApiKey()).getMetaKey();

        Collection<String> others = new ArrayList<>();
        others.add(resultB);
        trackEP.putCrossReference(resultA, others, "rlxName", su.getApiKey(), su.getApiKey());

        others = new ArrayList<>();
        others.add(resultA);
        trackEP.putCrossReference(resultB, others, "rlxNameB", su.getApiKey(), su.getApiKey());

        mediationFacade.purge(fortress.getName(), su.getApiKey());
        assertNull ( trackService.getHeader(resultA) );
        assertNull ( trackService.getHeader(resultB) );

    }

}
