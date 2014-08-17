package com.auditbucket.test.functional;

import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TrackTagRelationship;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import com.auditbucket.search.endpoint.ElasticSearchEP;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.MetaSearchSchema;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.MetaHeader;
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
    public void testMappingJson() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        // These are the minimum objects necessary to create a MetaHeader data
        Fortress fortress = new FortressNode(new FortressInputBean("fort", false), new CompanyNode("comp")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        MetaInputBean mib = getMetaInputBean(doc, user, "zzaa99", now);

        MetaHeader header = new MetaHeaderNode("zzUnique", fortress, mib, doc, user);

        SearchChange change = new MetaSearchChange(header);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<TrackTag> tags = new ArrayList<>();

        tags.add(new TrackTagRelationship(66l, new TagNode(new TagInputBean("my Tag", "TheLabel", "rlxname"))));
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
        doEsFieldQuery(header.getIndexName(), "@tag.my tag.key", "mytag", 1);
        assertNotNull(json);

    }

    private MetaInputBean getMetaInputBean(DocumentTypeNode docType, FortressUser fortressUser, String callerRef, DateTime now) {

        return new MetaInputBean(fortressUser.getFortress().getName(),
                fortressUser.getCode(),
                docType.getName(),
                now,
                callerRef);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        Fortress fortress = new FortressNode(new FortressInputBean("fort2", false), new CompanyNode("comp2")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        MetaInputBean mib = getMetaInputBean(doc, user, now.toString(), now);
        mib.setDescription("This is a description");

        MetaHeader header = new MetaHeaderNode(Long.toString(now.getMillis()), fortress, mib, doc, user);

        deleteEsIndex(header.getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                  MetaSearchSchema.WHAT_CODE, "AZERTY");
        what.put( MetaSearchSchema.WHAT_NAME, "NameText");
        what.put( MetaSearchSchema.WHAT_DESCRIPTION, "This is a description");
        LogInputBean log = new LogInputBean(user.getCode(), now, what);
        mib.setLog(log);
        SearchChange change = new MetaSearchChange(header);
        change.setWhat(what);

        SearchChange searchResult = trackRepo.update(change);
        assertNotNull(searchResult);
        Thread.sleep(1000);
        String mapping = getMapping(header.getIndexName());
        doEsQuery(header.getIndexName(), "AZERTY", 1);

        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "des", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.DESCRIPTION, "des", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "de", 0);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "descripti", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "descriptio", 1);
        // ToDo: Figure out ngram mappings
//        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "is is a de", 1);

        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "name", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "nam", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "nametext", 1);

        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "az", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "azer", 1);
        doEsTermQuery(header.getIndexName(), MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "azerty", 0);

    }
    MetaHeader getMetaHeader (String comp, String fort, String userName) throws DatagioException {
        // These are the minimum objects necessary to create a MetaHeader data
        Fortress fortress = new FortressNode(new FortressInputBean(fort, false), new CompanyNode(comp)) ;
        FortressUser user = new FortressUserNode(fortress, userName);
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        MetaInputBean mib = getMetaInputBean(doc, user, now.toString(), now);

        return new MetaHeaderNode(now.toString(), fortress, mib, doc, user);
    }

    @Test
    public void testCustomMappingWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        MetaHeader headerA = getMetaHeader("cust", "fort", "anyuser");
        MetaHeader headerB = getMetaHeader("cust", "fortb", "anyuser");

        SearchChange changeA = new MetaSearchChange(headerA, json);
        SearchChange changeB = new MetaSearchChange(headerB, json);

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
        doEsFieldQuery(headerA.getIndexName(), "@description", "test description", 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doEsFieldQuery(headerB.getIndexName(), "@description", "test description", 0);

    }




}
