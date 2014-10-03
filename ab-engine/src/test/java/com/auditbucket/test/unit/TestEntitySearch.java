package com.auditbucket.test.unit;

import com.auditbucket.engine.repo.neo4j.model.*;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.TrackTag;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 1/10/14
 * Time: 9:44 AM
 */
public class TestEntitySearch {
    @Test
    public void tags_ArrayWork() throws Exception{
        Collection<TrackTag> tags = new ArrayList<>();

        Entity e = getEntity("test", "blah", "asdf", "don'tcare");

        // ToDo: What is the diff between these relationships
        tags.add( new TrackTagRelationship(e, getTag("NameA", "dupe"), "dupe", null ));
        tags.add( new TrackTagRelationship(e, getTag("NameB", "Dupe"), "Dupe", null ));
        tags.add( new TrackTagRelationship(e, getTag("NameC", "dupe"), "dupe", null ));

        EntitySearchChange entitySearchChange = new EntitySearchChange(e);
        entitySearchChange.setTags(tags);
        assertEquals(1,entitySearchChange.getTagValues().size());
        // Find by relationship
        Map<String, Object> values = entitySearchChange.getTagValues().get("dupe");
        assertTrue (values.get("name") instanceof Collection);
        Collection mValues = (Collection) values.get("name");
        // Each entry has a Name and Code value
        assertEquals("Incorrect Values found for the relationship. Not ignoring case?", 3,mValues.size() );

        System.out.println(entitySearchChange.getTagValues());
    }

    Tag getTag (String tagName, String rlxName){
        TagInputBean tagInputBean = new TagInputBean(tagName, rlxName);
        return new TagNode(tagInputBean);
    }

    Entity getEntity(String comp, String fort, String userName, String doctype) throws FlockException {
        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean(fort, false), new CompanyNode(comp));
        FortressUser user = new FortressUserNode(fortress, userName);
        DocumentTypeNode doc = new DocumentTypeNode(fortress, doctype);

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, now.toString(), now);

        return new EntityNode(now.toString(), fortress, mib, doc, user);

    }

    EntityInputBean getEntityInputBean(DocumentTypeNode docType, FortressUser fortressUser, String callerRef, DateTime now) {

        return new EntityInputBean(fortressUser.getFortress().getName(),
                fortressUser.getCode(),
                docType.getName(),
                now,
                callerRef);

    }
}
