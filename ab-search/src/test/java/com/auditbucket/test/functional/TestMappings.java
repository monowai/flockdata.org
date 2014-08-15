package com.auditbucket.test.functional;

import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TrackTagRelationship;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import com.auditbucket.search.endpoint.ElasticSearchEP;
import com.auditbucket.search.model.MetaSearchChange;
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
public class TestMappings extends ESBase{
    @Autowired
    TrackSearchDao trackRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void testMappingJson() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);


        Company company = new CompanyNode("comp");
        Fortress fortress = new FortressNode(new FortressInputBean("fort", false), company);
        FortressUser user = new FortressUserNode(fortress, "mikey");
        user.setFortress(fortress);

        DateTime now = new DateTime();
        MetaInputBean mib = new MetaInputBean(fortress.getName(), user.getCode(), "mappingtest", now, "zzaa99");

        //ToDo: DocumentType is dodgy - in the MIB and a parameter
        MetaHeader header = new MetaHeaderNode("zzUnique", fortress, mib, new DocumentTypeNode(mib.getDocumentType()));

        header.setFortressLastWhen(now.getMillis());

        header.setLastUser(user);
        header.setCreatedBy(user);

        SearchChange change = new MetaSearchChange(header);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<TrackTag> tags = new ArrayList<>();

        tags.add(new TrackTagRelationship(66l, new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"))));
        change.setTags(tags);

        deleteEsIndex(header.getIndexName());

        change = trackRepo.update(change);
        assertNotNull(change);
        assertNotNull(change.getSearchKey());
        header.setSearchKey(change.getSearchKey());
        json = trackRepo.findOne(header);
        assertNotNull(json);

    }



}
