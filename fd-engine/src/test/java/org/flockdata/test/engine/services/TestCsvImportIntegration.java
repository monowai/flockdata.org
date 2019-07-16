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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import junit.framework.TestCase;
import org.flockdata.data.Entity;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.track.service.FdServerIo;
import org.flockdata.integration.FileProcessor;
import org.flockdata.integration.Template;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.Store;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.flockdata.test.unit.client.FdTemplateMock;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Creates a threaded Entity importer to debug suspicious behaviour identified
 * when importing a particular dataset. Periodically we see extra logs get created
 * <p>
 * This is a useful test to demonstrate how you can emulate the CSV import functionality
 * in a functional test
 *
 * @author mholdsworth
 * @tag Test, Track, DelimitedFile
 * @since 10/10/2014
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    Neo4jConfigTest.class,
    FdTemplateMock.class,
    FdServerIo.class,
    MapBasedStorageProxy.class})
@ActiveProfiles( {"fd-auth-test", "dev"})
public class TestCsvImportIntegration extends EngineBase {

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    private Template fdTemplate;

    @Override
    public void cleanUpGraph() {
        super.cleanUpGraph();   // DAT-363
    }

    @Test
    public void csvImport_DuplicateLogsNotCreated() throws Exception {
        assertNotNull(fdTemplate);
        cleanUpGraph(); // No transaction so need to clear down the graph

        logger.debug("### csvImport_DuplicateLogsNotCreated");
        engineConfig.setTestMode(true);
        assertTrue(engineConfig.store().equals(Store.MEMORY));
        setSecurity();
        final SystemUser su = registerSystemUser("importSflow", mike_admin);

        FortressNode f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("StackOverflow", true));
        DocumentNode docType = conceptService.resolveByDocCode(f, "QuestionEvent");
        int i = 1, maxRuns = 4;
        do {
            fileProcessor.processFile(new ExtractProfileHandler(ContentModelDeserializer.getContentModel("/models/test-sflow.json")), "/data/test-sflow.csv");
            Thread.yield();
            Entity entityA = entityService.findByCode(su.getCompany(), f.getName(), docType.getName(), "563890");
            assertNotNull(entityA);
            TestCase.assertEquals("563890", entityA.getSegment().getCode());

            EntityLog log = entityService.getLastEntityLog(entityA.getId());
            Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entityA.getKey());
            for (EntityLogResult entityLog : logs) {
                logger.debug("{}, {}", new DateTime(entityLog.getWhen()), entityLog.getChecksum());
            }
            logger.debug("entity.Log When {}", new DateTime(log.getFortressWhen()));
            Thread.yield();
            assertEquals("Run " + i + " Log was not set to the most recent", new DateTime(1235020128000l), new DateTime(log.getFortressWhen()));
            assertEquals("Run " + i + " has wrong log count", 6, entityService.getLogCount(su.getCompany(), entityA.getKey()));
            i++;
        } while (i <= maxRuns);
    }


}
