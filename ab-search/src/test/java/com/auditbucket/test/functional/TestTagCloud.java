package com.auditbucket.test.functional;

import com.auditbucket.engine.repo.neo4j.model.*;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.search.dao.QueryDaoES;
import com.auditbucket.search.endpoint.ElasticSearchEP;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.TagCloud;
import com.auditbucket.search.model.TagCloudParams;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackSearchDao;
import com.auditbucket.track.model.TrackTag;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Map;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:53 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestTagCloud extends ESBase {
    @Autowired
    TrackSearchDao trackRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Autowired
    QueryDaoES queryDaoES;

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean("fort1", false), new CompanyNode("comp")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, "zzaa99", now);

        Entity entity = new EntityNode("zzUnique", fortress, mib, doc, user);

        SearchChange change = new EntitySearchChange(entity);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<TrackTag> tags = new ArrayList<>();

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        tags.add(new TrackTagRelationship(66l, tag));
        change.setTags(tags);

        deleteEsIndex(entity.getIndexName());

        trackRepo.update(change);
        Thread.sleep(1000);
        TagCloudParams tagCloudParams = new TagCloudParams();
        tagCloudParams.setCompany(entity.getFortress().getCompany().getName());
        tagCloudParams.setFortress(entity.getFortress().getName());
        tagCloudParams.setType(entity.getDocumentType());

        TagCloud tagCloud = queryDaoES.getCloudTag(tagCloudParams);
        // ToDo: Fix this
//        assertEquals(20, tagCloud.getTerms().get("now").intValue());
//        assertEquals(20, tagCloud.getTerms().get("is").intValue());
//        assertEquals(20, tagCloud.getTerms().get("time").intValue());
//        assertEquals(20, tagCloud.getTerms().get("for").intValue());
//        assertEquals(20, tagCloud.getTerms().get("all").intValue());
//        assertEquals(20, tagCloud.getTerms().get("good").intValue());
//        assertEquals(20, tagCloud.getTerms().get("men").intValue());
//        assertEquals(20, tagCloud.getTerms().get("come").intValue());
//        assertEquals(20, tagCloud.getTerms().get("aid").intValue());
//        assertEquals(20, tagCloud.getTerms().get("of").intValue());
//        assertEquals(20, tagCloud.getTerms().get("party").intValue());
//        assertEquals(1, tagCloud.getTerms().get("my").intValue());
//        assertEquals(1, tagCloud.getTerms().get("tag").intValue());

        // TODO to get the tag cloud working this asserts must wrok and be uncommented
        //assertEquals(60, tagCloud.getTerms().get("the").intValue());
        //assertEquals(40, tagCloud.getTerms().get("to").intValue());

    }


}
