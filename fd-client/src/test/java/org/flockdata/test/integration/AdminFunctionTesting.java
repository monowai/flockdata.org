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
import org.flockdata.client.commands.AdminPurge;
import org.flockdata.client.commands.EntityGet;
import org.flockdata.client.commands.SearchFdPost;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.*;
import org.flockdata.test.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
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
public class AdminFunctionTesting {

    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    IntegrationHelper integrationHelper;

    @Autowired
    private AmqpServices amqpServices;

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    private FdRestWriter fdRestWriter;

    private static Logger logger = LoggerFactory.getLogger(AdminFunctionTesting.class);

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
        qp.setTypes("SearchDoc");
        SearchFdPost search = new SearchFdPost(clientConfiguration, fdRestWriter, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit", 1, search.result().getResults().size());

        AdminPurge purge = new AdminPurge(clientConfiguration, fdRestWriter, "purgeFortressRemovesEsIndex" );
        purge.exec();
        Thread.sleep (3000); // let things happen
        assertNull("The entity should not exist because the fortress was purged", entityGet.exec().result());

        assertEquals("The entity search doc was not removed", 0,search.exec().result().getResults().size());



    }

}
