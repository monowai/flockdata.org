package com.auditbucket.test.unit;

import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.engine.repo.neo4j.model.FortressNode;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TagNode;
import com.auditbucket.engine.repo.neo4j.model.TrackTagRelationship;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.TrackTag;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;

/**
 * User: mike
 * Date: 4/08/14
 * Time: 4:34 PM
 */
public class HashcodeAndEqualityChecks {
    public HashcodeAndEqualityChecks() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Test
    public void tagNodes() throws Exception{


        // We don't compare the relationships primary key for a tag
        TagNode tagNode = getTag("Samsung", "plantif", 12345l);
        TagNode tagNodeB =  getTag("Samsung", "plantif", 12345l);

        assertEquals(tagNode, tagNodeB);
        ArrayList<TagNode> tags = new ArrayList<>();
        tags.add(tagNode);
        assertEquals(true, tags.contains(tagNodeB));

    }

    private TagNode getTag(String name, String relationship, Long l) {
        TagInputBean tagInputBean = new TagInputBean(name, relationship);
        TagNode tagNode = new TagNode(tagInputBean);
        tagNode.setId(l);
        return tagNode;
    }

    @Test
    public void trackTags() throws Exception{

        TagNode tagNode = getTag("Samsung", "plantif", 12345l);
        TagNode tagNodeB = getTag("Apple", "defendant", 12343l);

        CompanyNode company = new CompanyNode("TestCo");
        company.setId(12313);
        FortressNode fortress = new FortressNode(new FortressInputBean("Testing",true ), company);
        DocumentTypeNode documentTypeNode = new DocumentTypeNode(fortress, "DocTest");
        MetaInputBean metaInput = new MetaInputBean();
        metaInput.setCallerRef("abc");

        MetaHeaderNode mh = new MetaHeaderNode("123abc", fortress, metaInput, documentTypeNode);
        TrackTagRelationship trackTagA = new TrackTagRelationship(mh.getId(), tagNode);
        TrackTagRelationship trackTagB = new TrackTagRelationship(mh.getId(), tagNodeB);

        ArrayList<TrackTag>existingTags = new ArrayList<>();
        existingTags.add(trackTagA);
        existingTags.add(trackTagB);
        assertEquals(true, existingTags.contains(trackTagA));
        assertEquals(true, existingTags.contains(trackTagB));

    }
}
