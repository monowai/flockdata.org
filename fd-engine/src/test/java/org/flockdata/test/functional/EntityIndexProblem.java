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

import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.DocumentType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

/**
 * User: mike
 * Date: 10/10/14
 * Time: 9:37 AM
 */
public class EntityIndexProblem extends EngineBase {

    @Autowired
    private
    ImportProfileService importProfile;
    @Test
    public void stoppingAnErrorOccurring(){
        assertTrue(true);
    }

    //@Test
    public void simulate_LargishLoad () throws Exception {
        SystemUser su = registerSystemUser("blahxx");
        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("blah..", true));
        int max = 1000;
        int transBatch =2;
        int tagCount = 5;
        int i=0;
        Collection <TagInputBean> tags = new ArrayList<>();
        for (int tag = 0; tag < tagCount; tag++) {
            tags.add(new TagInputBean("Tag" + tag, "test" + tag));
        }
        StopWatch watch = new StopWatch("Starting");
        watch.start();
        while ( i < max){
            List<EntityInputBean> beans = new ArrayList<>();
            for (int x = 0; x < transBatch; x++) {
                EntityInputBean eib = new EntityInputBean(f.getName(), "test"+i, "zz", new DateTime());
                ContentInputBean cib = new ContentInputBean("answer"+i, new DateTime());
                cib.setWhat(Helper.getRandomMap());
                eib.setContent(cib);
                eib.setTags(tags);
                beans.add(eib);

            }
            mediationFacade.trackEntities(su.getCompany(), beans);
            i++;
            if (i % 100 == 0 )
                logger.info("Processed {} of {}", i*transBatch, max*transBatch);
        }
        watch.stop();
        double totalRows = transBatch * max;

        logger.info("Finished {}Transactions Per Second {} - avg processing time {}", watch.prettyPrint(), watch.getLastTaskTimeMillis()/totalRows, totalRows/watch.getLastTaskTimeMillis());
    }

//    @Test
    public void import_StackOverflowDebug() throws Exception {

        setSecurity("mike");
        SystemUser su = registerSystemUser("importSflow", mike_admin);

        Fortress f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("StackOverflow", true));
        DocumentType docType = schemaService.resolveByDocCode(f, "QuestionEvent");
        importProfile.save(f, docType, Helper.getImportParams("/sflow.json") );

        importProfile.process(su.getCompany(), f, docType, "/Users/mike/Downloads/answers.csv", false);
        //importProfile.process(su.getCompany(), f, docType, "/data/sflow/sm-answers.csv");
    }
}
