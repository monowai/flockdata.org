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

package org.flockdata.test.functional;

import org.flockdata.helper.FlockException;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.TrackBatcher;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void csvImport_DuplicateLogsNotCreated() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
//        engineConfig.setConceptsEnabled(false);
        logger.info("Starting ## csvImport_DuplicateLogsNotCreated");
        engineConfig.setTestMode(true);
        assertTrue(engineConfig.getKvStore().equals(KvService.KV_STORE.MEMORY));
        setSecurity();
        final SystemUser su = registerSystemUser("importSflow", mike_admin);

        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("StackOverflow", true));
        DocumentType docType = schemaService.resolveByDocCode(f, "QuestionEvent");
        int i = 1, maxRuns = 4;
        do {

            FileProcessor myProcessor = new FileProcessor();
            FdTestWriter testWriter = new FdTestWriter(su);

            ClientConfiguration defaults = new ClientConfiguration();

            myProcessor.processFile(Helper.getImportParams("/sflow.json"), "/sflow.csv", 0, testWriter, su.getCompany(), defaults);
            Thread.yield();
            Entity entityA = trackService.findByCallerRef(su.getCompany(), f.getName(), docType.getName(), "563890");
            assertNotNull(entityA);

            EntityLog log = trackService.getLastEntityLog(entityA.getId());
            Collection<EntityLog> logs = trackService.getEntityLogs(su.getCompany(), entityA.getMetaKey());
            for (EntityLog entityLog : logs) {
                logger.info("{}, {}", new DateTime(entityLog.getFortressWhen()), entityLog.getLog().getChecksum());
            }
            logger.debug("entity.Log When {}", new DateTime(log.getFortressWhen()));
            Thread.yield();
            assertEquals("Run " + i + " Log was not set to the most recent", new DateTime(1235020128000l), new DateTime(log.getFortressWhen()));
            assertEquals( "Run "+i+" has wrong log count", 6, trackService.getLogCount(su.getCompany(), entityA.getMetaKey()));
            i++;
        } while (i <= maxRuns);
    }

    private class FdTestWriter implements FdWriter {
        FdTestWriter(SystemUser su) {
            this.su = su;

        }

        int count = 0;

        SystemUser su = null;

        public SystemUserResultBean me() {
            return new SystemUserResultBean(su);
        }

        public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            return null;
        }

        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            if (count == 0)
                count = entityBatch.size();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 10000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));

            //if (configuration.getBatchSize() == 1) {
                for (EntityInputBean entityInputBean : entityBatch) {


                    //MyRunner runner = new MyRunner(entityInputBean, su.getApiKey());
                    //executor.execute(runner);
                    try {
                        if (entityInputBean != null) {
                            logger.debug("My Date {}", entityInputBean.getWhen());
                            mediationFacade.trackEntity(entityInputBean, su.getApiKey());
                        }
                    } catch (InterruptedException | FlockException | ExecutionException | IOException e) {
                        logger.error("Unexpected", e);
                    }
                }


            logger.debug("Executor at {}", executor.getActiveCount());

            return "";
        }

        @Override
        public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
            return 0;
        }

        @Override
        public boolean isSimulateOnly() {
            return false;
        }

        @Override
        public void close(TrackBatcher trackBatcher) throws FlockException {
            trackBatcher.flush();
        }
    }

//    private class MyRunner implements Runnable {
//        private EntityInputBean entityInputBean;
//        Collection<EntityInputBean> entityInputBeans;
//        private String apiKey;
//
//        MyRunner(EntityInputBean entityInputBean, String apiKey) {
//            this.entityInputBean = entityInputBean;
//            this.apiKey = apiKey;
//        }
//
//        @Override
//        public void run() {
//
//            try {
//                if (entityInputBean != null) {
//                    logger.debug("My Date {}", entityInputBean.getWhen());
//                    mediationFacade.trackEntity(entityInputBean, apiKey);
//                } else {
//                    mediationFacade.trackEntity(entityInputBean, apiKey);
//                }
//            } catch (InterruptedException | FlockException | ExecutionException | IOException e) {
//                logger.error("Unexpected", e);
//            }
//
//
//        }
//    }


}
