/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.integration;

import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.client.commands.AdminPurgeFortress;
import org.flockdata.client.commands.AdminPurgeFortressSegment;
import org.flockdata.client.commands.EntityGet;
import org.flockdata.client.commands.SearchFdPost;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Admin functions that rely on integration
 *
 * Created by mike on 12/05/16.
 */
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        FdBatcher.class,
        FdRestWriter.class,
        Exchanges.class,
        FileProcessor.class,
        IntegrationHelper.class,
        AmqpRabbitConfig.class,
        AmqpServices.class

})
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ActiveProfiles("fd-server")
public class IntAdminFunction {


    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    IntegrationHelper integrationHelper;

    @Autowired
    private AmqpServices amqpServices;

    @Before
    public void setupServices(){
        integrationHelper.waitForServices();
    }

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    private FdRestWriter fdRestWriter;

    private static Logger logger = LoggerFactory.getLogger(IntAdminFunction.class);

    @Test
    public void purgeFortressRemovesEsIndex() throws Exception {
        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeFortressRemovesEsIndex")
                        .setSearchEnabled(true))
                .setCode("SearchDoc")
                .setDocumentType(new DocumentTypeInputBean("DeleteSearchDoc"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.waitForEntityKey(logger, entityGet);
        integrationHelper.waitForSearch(logger, entityGet, 1);
        Thread.sleep(2000); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeFortressRemovesEsIndex");
        qp.setTypes("DeleteSearchDoc");
        SearchFdPost search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit", 1, search.result().getResults().size());

        AdminPurgeFortress purge = new AdminPurgeFortress(clientConfiguration, fdRestWriter, "purgeFortressRemovesEsIndex" );
        purge.exec();
        Thread.sleep (3000); // let things happen
        assertNull("The entity should not exist because the fortress was purged", entityGet.exec().result());

        assertEquals("The entity search doc was not removed", 0,search.exec().result().getResults().size());



    }

    @Test
    public void purgeSegmentRemovesOnlyTheSpecifiedOne() throws Exception{
        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        DocumentTypeInputBean docType = new DocumentTypeInputBean("DeleteSearchDoc");

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.waitForEntityKey(logger, entityGet);
        integrationHelper.waitForSearch(logger, entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.waitForEntityKey(logger, entityGet);
        integrationHelper.waitForSearch(logger, entityGet, 1);

        Thread.sleep(1000); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeSegment");
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(clientConfiguration,fdRestWriter, "purgeSegment", docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command failed to execute",search.exec().result());
        assertNotNull("Expected an index not found type error",search.result().getFdSearchError());
        assertTrue ("2015 index should be reported as missing", search.result().getFdSearchError().contains("2015"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Find Me")));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertNull("Didn't expect an error", search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }

    /**
     * Much the same as purgeSegmentRemovesOnlyTheSpecifiedOne but ensures the functions
     * work for entities that have no logs
     *
     * @throws Exception
     */
    @Test
    public void purgeSegmentEntitiesWithNoLogs() throws Exception{

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());
        final String FORTRESS = "purgeSegmentEntitiesWithNoLogs";
        DocumentTypeInputBean docType = new DocumentTypeInputBean(FORTRESS);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.waitForEntityKey(logger, entityGet);
        integrationHelper.waitForSearch(logger, entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setEntityOnly(true);

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.waitForEntityKey(logger, entityGet);
        integrationHelper.waitForSearch(logger, entityGet, 1);

        Thread.sleep(1000); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress(FORTRESS);
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(clientConfiguration,fdRestWriter, FORTRESS, docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command failed to execute",search.exec().result());
        assertNotNull("Expected an index not found type error",search.result().getFdSearchError());
        assertTrue ("2015 index should be reported as missing", search.result().getFdSearchError().contains("2015"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertNull("Didn't expect an error", search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }
}
