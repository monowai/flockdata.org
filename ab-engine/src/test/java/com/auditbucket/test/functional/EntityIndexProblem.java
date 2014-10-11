package com.auditbucket.test.functional;

import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.DocumentType;
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

        importProfile.process(su.getCompany(), f, docType, "/Users/mike/Downloads/answers.csv");
        //importProfile.process(su.getCompany(), f, docType, "/data/sflow/sm-answers.csv");
    }
}
