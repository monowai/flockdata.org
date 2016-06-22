/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.engine.services;

import junit.framework.TestCase;
import org.flockdata.model.*;
import org.flockdata.profile.ImportContentModelDeserializer;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.shared.FileProcessor;
import org.flockdata.store.Store;
import org.flockdata.track.bean.EntityLogResult;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Creates a threaded Entity importer to debug suspicious behaviour identified
 * when importing a particular dataset. Periodically we see extra logs get created
 * <p/>
 * This is a useful test to demonstrate how you can emulate the CSV import functionality
 * in a functional test
 * <p/>
 * User: mike
 * Date: 10/10/14
 * Time: 9:37 AM
 */
public class TestCsvImportIntegration extends EngineBase {

    @Autowired
    FileProcessor fileProcessor;

    @Override
    public void cleanUpGraph() {
        super.cleanUpGraph();   // DAT-363
    }

    @Test
    public void csvImport_DuplicateLogsNotCreated() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
//        engineConfig.setConceptsEnabled(false);
        logger.debug("### csvImport_DuplicateLogsNotCreated");
        engineConfig.setTestMode(true);
        assertTrue(engineConfig.store().equals(Store.MEMORY));
        setSecurity();
        final SystemUser su = registerSystemUser("importSflow", mike_admin);

        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("StackOverflow", true));
        DocumentType docType = conceptService.resolveByDocCode(f, "QuestionEvent");
        int i = 1, maxRuns = 4;
        do {
            fileProcessor.processFile(ImportContentModelDeserializer.getContentModel("/models/test-sflow.json"), "/data/test-sflow.csv");
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
            assertEquals( "Run "+i+" has wrong log count", 6, entityService.getLogCount(su.getCompany(), entityA.getKey()));
            i++;
        } while (i <= maxRuns);
    }


}
