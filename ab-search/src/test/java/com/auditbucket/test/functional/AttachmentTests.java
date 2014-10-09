package com.auditbucket.test.functional;

import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.TrackSearchDao;
import com.auditbucket.search.endpoint.ElasticSearchEP;
import com.auditbucket.track.model.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static junit.framework.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/09/14
 * Time: 3:26 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class AttachmentTests extends ESBase {
    @Autowired
    TrackSearchDao trackRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void attachment_PdfIndexedAndFound() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entity = getEntity("cust", "fort", "anyuser");

        SearchChange changeA = new EntitySearchChange(entity, new ContentInputBean(json));
        changeA.setAttachment(Helper.getPdfDoc());

        // FortB will have
        changeA.setDescription("Test Description");

        deleteEsIndex(entity.getIndexName());

        changeA = trackRepo.update(changeA);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());
        doQuery(changeA.getIndexName(), "brown", 1);

    }



}
