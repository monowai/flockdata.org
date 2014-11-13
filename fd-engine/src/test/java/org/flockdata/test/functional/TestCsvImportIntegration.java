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
import org.flockdata.helper.JsonUtils;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private
    ImportProfileService importProfile;

    @Test
//    @Repeat(30)
    public void csvImport_DuplicateLogsNotCreated() throws Exception {
        logger.info("Starting ## csvImport_DuplicateLogsNotCreated");
        setSecurity();
        final SystemUser su = registerSystemUser("importSflow", mike_admin);

        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("StackOverflow", true));
        DocumentType docType = schemaService.resolveByDocCode(f, "QuestionEvent");

        FileProcessor myProcessor = new FileProcessor();
        FdTestWriter testWriter = new FdTestWriter(su);

        myProcessor.processFile(Helper.getImportParams("/sflow.json"), "/sflow.csv", 0, testWriter, su.getCompany(), false);
        Entity entityA = trackService.findByCallerRef(su.getCompany(), f.getName(), docType.getName(), "563890");
//        Entity entityB = trackService.findByCallerRef(su.getCompany(), f.getName(), docType.getName(), "563480");
        assertNotNull(entityA);
//        assertNotNull(entityB);

        EntityLog log = trackService.getLastEntityLog(entityA.getId());

        logger.debug("entity.Log When {}", new DateTime(log.getFortressWhen()));
        assertEquals("Log was not set to the most recent", new DateTime(1235020128000l), new DateTime(log.getFortressWhen()));
        assertEquals("First run through got the wrong log count", testWriter.count, trackService.getLogCount(su.getCompany(), entityA.getMetaKey()));

        logger.debug("@@@ Second run beginning");
        myProcessor.processFile(Helper.getImportParams("/sflow.json"), "/sflow.csv", 0, testWriter, su.getCompany(), false);
        log = trackService.getLastEntityLog(su.getCompany(), entityA.getMetaKey());
        logger.debug("logWhen {}, entity.Log When {}", entityA.getLastChange(), new DateTime(log.getFortressWhen()));
        //assertEquals();
        assertEquals("Second run through got the wrong log count", testWriter.count, trackService.getLogCount(su.getCompany(), entityA.getMetaKey()));

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
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, boolean async) throws FlockException {
            if (count == 0)
                count = entityBatch.size();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 10000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));
            for (EntityInputBean entityInputBean : entityBatch) {
                entityInputBean.setApiKey(su.getApiKey());

                MyRunner runner = new MyRunner(entityInputBean);
                executor.execute(runner);
            }
            while (executor.getActiveCount() != 0)
                logger.trace("Waiting for active count to hit 0");
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
        public Collection<Tag> getCountries() throws FlockException {
            return null;
        }
    }

    private class MyRunner implements Runnable {
        private EntityInputBean entityInputBean;

        MyRunner(EntityInputBean entityInputBean) {
            this.entityInputBean = entityInputBean;
        }

        @Override
        public void run() {

            try {
                logger.debug("My Date {}", entityInputBean.getWhen());
                mediationFacade.trackEntity(JsonUtils.getObjectAsJsonBytes(entityInputBean));
            } catch (InterruptedException | FlockException | ExecutionException | IOException e) {
                logger.error("Unexpected", e);
            }


        }
    }


}
