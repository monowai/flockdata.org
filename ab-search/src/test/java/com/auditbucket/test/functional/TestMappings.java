package com.auditbucket.test.functional;

import com.auditbucket.engine.repo.neo4j.model.*;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.search.endpoint.ElasticSearchEP;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.EntitySearchSchema;
import com.auditbucket.track.bean.ContentInputBean;
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

import static junit.framework.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:53 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestMappings extends ESBase {
    @Autowired
    TrackSearchDao trackRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean("fort", false), new CompanyNode("comp")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, "zzaa99", now);

        Entity header = new EntityNode("zzUnique", fortress, mib, doc, user);

        SearchChange change = new EntitySearchChange(header);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<TrackTag> tags = new ArrayList<>();

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        tags.add(new TrackTagRelationship(66l, tag));
        change.setTags(tags);


        deleteEsIndex(header.getIndexName());

        change = trackRepo.update(change);
        Thread.sleep(1000);
        assertNotNull(change);
        assertNotNull(change.getSearchKey());
        header.setSearchKey(change.getSearchKey());
        json = trackRepo.findOne(header);

        // In this test, @tag.*.code is ignored so it should find the value with a space in it
        // In prod we use the .key field in this manner
        doDefaultFieldQuery(header.getIndexName(), "@tag.mytag.code", "my tag", 1);
        assertNotNull(json);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        Fortress fortress = new FortressNode(new FortressInputBean("fort2", false), new CompanyNode("comp2")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, now.toString(), now);
        mib.setDescription("This is a description");

        Entity header = new EntityNode(Long.toString(now.getMillis()), fortress, mib, doc, user);

        deleteEsIndex(header.getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                  EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put( EntitySearchSchema.WHAT_NAME, "NameText");
        what.put( EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");
        ContentInputBean log = new ContentInputBean(user.getCode(), now, what);
        mib.setLog(log);
        SearchChange change = new EntitySearchChange(header);
        change.setWhat(what);

        SearchChange searchResult = trackRepo.update(change);
        assertNotNull(searchResult);
        Thread.sleep(2000);
        doQuery(header.getIndexName(), "AZERTY", 1);

        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "des", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.DESCRIPTION, "des", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "de", 0);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "descripti", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "descriptio", 1);
        // ToDo: Figure out ngram mappings
//        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "is is a de", 1);

        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "name", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "nam", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "nametext", 1);

        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "az", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "azer", 1);
        doTermQuery(header.getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "azerty", 0);

    }


    @Test
    public void testCustomMappingWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity headerA = getEntity("cust", "fort", "anyuser");
        Entity headerB = getEntity("cust", "fortb", "anyuser");

        SearchChange changeA = new EntitySearchChange(headerA, json);
        SearchChange changeB = new EntitySearchChange(headerB, json);

        // FortB will have
        changeA.setDescription("Test Description");
        changeB.setDescription("Test Description");

        deleteEsIndex(headerA.getIndexName());
        deleteEsIndex(headerB.getIndexName());

        changeA = trackRepo.update(changeA);
        changeB = trackRepo.update(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        // by default we analyze the @description field
        doDefaultFieldQuery(headerA.getIndexName(), "@description", changeA.getDescription(), 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doDefaultFieldQuery(headerB.getIndexName(), "@description", changeB.getDescription(), 0);

    }

    @Test
    public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity headerA = getEntity("cust", "fort", "anyuser", "fortdoc");
        Entity headerB = getEntity("cust", "fort", "anyuser", "doctype");


        SearchChange changeA = new EntitySearchChange(headerA, json);
        SearchChange changeB = new EntitySearchChange(headerB, json);

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        ArrayList<TrackTag> tags = new ArrayList<>();
        tags.add(new TrackTagRelationship(66l, tag));
        changeA.setTags(tags);
        changeB.setTags(tags);

        deleteEsIndex(headerA.getIndexName());
        deleteEsIndex(headerB.getIndexName());

        changeA = trackRepo.update(changeA);
        changeB = trackRepo.update(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        doDefaultFieldQuery(headerA.getIndexName(), headerA.getDocumentType().toLowerCase(), "@tag.mytag.code", "my tag", 1);
        doDefaultFieldQuery(headerB.getIndexName(), headerB.getDocumentType().toLowerCase(), "@tag.mytag.code", "my tag", 1);
        doDefaultFieldQuery(headerB.getIndexName(), "@tag.mytag.code", "my tag", 2);

    }


}
