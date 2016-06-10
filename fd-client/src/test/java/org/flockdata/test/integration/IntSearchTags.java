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
import org.flockdata.client.commands.SearchEsPost;
import org.flockdata.client.commands.TagsGet;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.*;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Establishes the integration test environment. Descendant classes use @Test functions against
 * this established stack
 * <p>
 * Created by mike on 3/04/16.
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
public class IntSearchTags {


    // Uncomment this ClassRue to run the stack only for this class
    //  otherwise leave it commented and run the Suite
//    @ClassRule
//    public static FdDocker stack = new FdDocker();

    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    IntegrationHelper integrationHelper;

    @Autowired
    private AmqpServices amqpServices;

    private SearchHelper searchHelper = new SearchHelper();

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

    private static Logger logger = LoggerFactory.getLogger(IntSearchTags.class);


    @Test
    public void simpleTags() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        Collection<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagInputBean = new TagInputBean("ACode", "AnyLabel");
        tags.add(tagInputBean);
        tagInputBean = new TagInputBean("BCode", "AnyLabel");
        tags.add(tagInputBean);

        amqpServices.publishTags(tags);

        integrationHelper.shortSleep();  // Async delivery, so lets wait a bit....
        TagsGet entityGet = new TagsGet(clientConfiguration, fdRestWriter, "AnyLabel");

        TagResultBean[] foundTags = entityGet.exec().result();
        assertNotNull(foundTags);

        integrationHelper.shortSleep();
        QueryParams qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "*");

        SearchEsPost search = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        integrationHelper.assertWorked("Search Reply ", search);

        Map<String,Object> esResult = search.result();
        assertFalse ( "errors were found "+esResult.get("errors") ,esResult.containsKey("errors"));

        searchHelper.assertHitCount("Expected 2 hits for two tags when searching for *", 2, search.result());

        // Assert that updates work
        tags.clear();
        tagInputBean = new TagInputBean("ACode", "AnyLabel")
                        .setName("acode wonder")
                        .setMerge(true);

        tagInputBean.setProperty("aprop", 123) ;

        tags.add(tagInputBean);
        amqpServices.publishTags(tags);
        Thread.sleep(2000);  // Async delivery, so lets wait a bit....

        qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "wonder");

        search = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        integrationHelper.longSleep();

        integrationHelper.assertWorked("Search Reply ", search);
        searchHelper.assertHitCount("Expected single hit for an updated tag", 1, search.result());

        String json = searchHelper.getHits(search.result());
        assertNotNull(json);
        assertTrue ("ACode tag should have been in the result", json.contains("ACode"));
        assertTrue ("Didn't find correct search text", json.contains("acode wonder"));

    }

    @Test
    public void bulkTagsDontBlock() throws Exception {
        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());
        Collection<TagInputBean>setA = getRandomTags("codea", "Set");
        Collection<TagInputBean>setB = getRandomTags("codea", "Set");
        Collection<TagInputBean>setC = getRandomTags("codea", "Set");
        Collection<TagInputBean>setD = getRandomTags("codea", "Set");
        amqpServices.publishTags(setA);
        amqpServices.publishTags(setB);
        amqpServices.publishTags(setC);
        amqpServices.publishTags(setD);
        integrationHelper.longSleep();
        QueryParams qp = searchHelper.getTagQuery("Set*", "code*");
        SearchEsPost search = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        search.exec();
        integrationHelper.assertWorked("Not finding any tags",search);


    }

    private Collection<TagInputBean>getRandomTags(String code, String label){
        int i =0;
        int max = 20;

        Collection<TagInputBean> tags = new ArrayList<>();
        while ( i< max){
            TagInputBean tagInputBean = new TagInputBean(code+i, label+i);
            tags.add(tagInputBean);

            i++;
        }
        return tags;
    }

}
