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

import com.auditbucket.company.endpoint.CompanyEP;
import com.auditbucket.engine.repo.neo4j.model.FortressNode;
import com.auditbucket.engine.service.*;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.track.service.*;
import com.auditbucket.geography.service.GeographyService;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import static org.junit.Assert.*;

/**
 * User: mike Date: 16/06/14 Time: 7:54 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:root-context.xml",
		"classpath:apiDispatcher-servlet.xml" })
@Ignore
public class EngineBase {

	@Autowired
	protected RegistrationService regService;

	@Autowired
    SchemaService schemaService;

	@Autowired
    protected
    FortressService fortressService;

	@Autowired
    protected
    TrackService trackService;

	@Autowired
    EntityTagService entityTagService;

    @Autowired
    GeographyService geoService;

    @Qualifier("mediationFacadeNeo4j")
    @Autowired
    protected
    MediationFacade mediationFacade;

    @Autowired
    TxService txService;

    @Autowired
    protected
    LogService logService;

	@Autowired
    TrackEventService trackEventService;

	@Autowired
    SystemUserService systemUserService;

	@Autowired
    TagService tagService;

	@Autowired
    public
    EngineConfig engineConfig;

	@Autowired
    QueryService queryService;

	@Autowired
    KvService kvService;

	@Autowired
	CompanyService companyService;

	@Autowired
    @Deprecated // Use companyService instead
            CompanyEP companyEP;

	@Autowired
    SearchServiceFacade searchService;

	@Autowired
	Neo4jTemplate template;

	@Autowired
	SecurityHelper securityHelper;

	static Logger logger = LoggerFactory.getLogger(EngineBase.class);

	// These have to be in test-security.xml in order to create SysUserRegistrations
    protected static final String sally_admin = "sally";
	protected static final String mike_admin = "mike"; // Admin role
    protected static final String harry = "harry";

	Authentication authDefault = new UsernamePasswordAuthenticationToken(
			mike_admin, "123");

	public Fortress createFortress(SystemUser su) throws Exception {
		return fortressService.registerFortress(su.getCompany(), new FortressInputBean("" + System.currentTimeMillis()));
	}


    @Rollback(false)
	@BeforeTransaction
	public void cleanUpGraph() {
		Neo4jHelper.cleanDb(template);
		engineConfig.setConceptsEnabled(false);
		engineConfig.setDuplicateRegistration(true);
	}

	@Before
	public void setSecurity() {
		engineConfig.setMultiTenanted(false);
		SecurityContextHolder.getContext().setAuthentication(authDefault);
	}

	public static void setSecurity(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	public static Authentication setSecurity(String userName) {
		Authentication auth = new UsernamePasswordAuthenticationToken(userName, "123");
		setSecurity(auth);
		return auth;
	}

	public static void setSecurityEmpty() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	Transaction beginManualTransaction() {
		return template.getGraphDatabase().beginTx();
	}

	void commitManualTransaction(Transaction t) {
		t.success();
		t.close();
	}
    public SystemUser registerSystemUser() throws Exception{
        return registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);

    }
    public SystemUser registerSystemUser(String companyName) throws Exception{
        return  registerSystemUser(companyName, Long.toHexString(System.currentTimeMillis()));
    }
    public SystemUser registerSystemUser(String companyName, String accessUser) throws Exception{
        Company company = companyService.findByName(companyName);
        if ( company == null ) {
            logger.debug("Creating company {}", companyName);
            company = companyService.create(companyName);
        }
        SystemUser su = regService.registerSystemUser(company, new RegistrationBean( accessUser).setIsUnique(false));
//        SystemUser su = regService.registerSystemUser(company, new RegistrationBean(companyName, accessUser).setIsUnique(false));
        logger.debug("Returning SU {}", su);
        return su;
    }


    public static void waitAWhile() throws Exception {
		waitAWhile(null, 1500);
	}

	public static void waitAWhile(int millis) throws Exception {
		waitAWhile(null, millis);
	}

	public static void waitAWhile(String message) throws Exception {
		String ss = System.getProperty("sleepSeconds");
		if (ss == null || ss.equals(""))
			ss = "1";
		if (message == null)
			message = "Slept for {} seconds";
		waitAWhile(message, Long.decode(ss) * 1000);
	}

	/**
	 * Processing delay for threads and integration to complete. If you start
	 * getting sporadic Heuristic exceptions, chances are you need to call this
	 * routine to give other threads time to commit their work. Likewise,
	 * waiting for results from ab-search can take a while. We can't know how
	 * long this is so you can experiment on your own environment by passing in
	 * -DsleepSeconds=1
	 *
	 * @param milliseconds
	 *            to pause for
	 * @throws Exception
	 */
	public static void waitAWhile(String message, long milliseconds)
			throws Exception {
		Thread.sleep(milliseconds);
		logger.trace(message, milliseconds / 1000d);
	}

	EntityLog waitForLogCount(Company company, Entity entity, int expectedCount) throws Exception {
		// Looking for the first searchKey to be logged against the entity
		int i = 0;
		int timeout = 100;
        int count = 0 ;
        //int sleepCount = 90;
        //logger.debug("Sleep Count {}", sleepCount);
        //Thread.sleep(sleepCount); // Avoiding RELATIONSHIP[{id}] has no property with propertyKey="__type__" NotFoundException
		while ( i <= timeout) {
            Entity updateEntity = trackService.getEntity(company, entity.getMetaKey());
            count = trackService.getLogCount(company, updateEntity.getMetaKey());

            EntityLog log = trackService.getLastEntityLog(company, updateEntity.getMetaKey());
            // We have at least one log?
			if ( count == expectedCount )
				return log;
			Thread.yield();
			if (i > 20)
				waitAWhile("Waiting for the log to update {}");
			i++;
		}
		if (i > 22)
			logger.info("Wait for log got to [{}] for metaId [{}]", i,
                    entity.getId());
        throw new Exception(String.format("Timeout waiting for the defined log count of %s. We found %s", expectedCount, count));
	}
    long waitForFirstLog(Company company, Entity source) throws Exception {
        // Looking for the first searchKey to be logged against the entity
        long thenTime = System.currentTimeMillis();
        int i = 0;

        Entity entity = trackService.getEntity(company, source.getMetaKey());

        int timeout = 100;
        while ( i <= timeout) {
            EntityLog log = trackService.getLastEntityLog(company, entity.getMetaKey());
            if (log != null )
                return i;
            Thread.yield();
            if (i > 20)
                waitAWhile("Waiting for the log to arrive {}");
            i++;
        }
        if (i > 22)
            logger.info("Wait for log got to [{}] for metaId [{}]", i,
                    entity.getId());
        return System.currentTimeMillis() - thenTime;
    }


	public void testJson() throws Exception {
		FortressNode fortressNode = new FortressNode(new FortressInputBean(
				"testing"), new CompanyNode("testCompany"));
		byte[] bytes = JsonUtils.getObjectAsJsonBytes(fortressNode);
		Fortress f = JsonUtils.getBytesAsObject(bytes, FortressNode.class);
		assertNotNull(f);
		assertNull(f.getCompany());// JsonIgnored - Discuss!
		assertEquals("testing", f.getName());
	}
	



}
