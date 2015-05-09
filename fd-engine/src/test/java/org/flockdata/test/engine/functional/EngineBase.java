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

import org.flockdata.company.endpoint.CompanyEP;
import org.flockdata.company.model.CompanyNode;
import org.flockdata.company.model.FortressNode;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.schema.service.TxService;
import org.flockdata.engine.track.service.TrackEventService;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.kv.FdKvConfig;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.registration.service.SystemUserService;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.service.*;
import org.junit.Before;
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
public abstract class EngineBase {

	@Autowired
	protected RegistrationService regService;

	@Autowired
    SchemaService schemaService;

	@Autowired
    protected
    FortressService fortressService;

	@Autowired
    protected
    EntityService entityService;

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
    PlatformConfig engineConfig;

	@Autowired
    QueryService queryService;

	@Autowired
    KvService kvService;

	@Autowired
	FdKvConfig kvConfig;

	@Autowired
	public CompanyService companyService;

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
		return fortressService.registerFortress(su.getCompany(), new FortressInputBean("" + System.currentTimeMillis(), true));
	}


    @Rollback(false)
	@BeforeTransaction
	public void cleanUpGraph() {
        // DAT-348 - override this if you're running a multi-threaded tests where multiple transactions
        //           might be started giving you sporadic failures.
		Neo4jHelper.cleanDb(template);
		engineConfig.setDuplicateRegistration(true);
	}

	@Before
	public void setSecurity() {
		engineConfig.setMultiTenanted(false);
        engineConfig.setStoreEnabled("true");
        engineConfig.setConceptsEnabled("false");
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
	 * waiting for results from fd-search can take a while. We can't know how
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
            Entity updateEntity = entityService.getEntity(company, entity.getMetaKey());
            count = entityService.getLogCount(company, updateEntity.getMetaKey());

            EntityLog log = entityService.getLastEntityLog(company, updateEntity.getMetaKey());
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

        Entity entity = entityService.getEntity(company, source.getMetaKey());

        int timeout = 100;
        while ( i <= timeout) {
            EntityLog log = entityService.getLastEntityLog(company, entity.getMetaKey());
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
