/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.junit.Test;

/**
 * Verifies that EntityInputBeans can be merged into each other
 *
 * @author mholdsworth
 * @since 23/01/2016
 */
public class TestEntityInputBean {

    @Test
    public void testMerge() throws Exception {
        DocumentTypeInputBean movieDoc = new DocumentTypeInputBean("Movie");
        EntityInputBean movie = new EntityInputBean()
            .setCode("tt0356910")
            .setDocumentType(movieDoc)
            .addTag(new TagInputBean("Doug Liman", "Person", new EntityTagRelationshipInput("DIRECTED")));


        EntityInputBean brad = new EntityInputBean()
            .setCode("tt0356910")
            .setDocumentType(movieDoc)
            .addTag(new TagInputBean("Brad Pitt", "Person", new EntityTagRelationshipInput("ACTED")));

        EntityInputBean angie = new EntityInputBean()
            .setCode("tt0356910")
            .setDocumentType(movieDoc)
            .addTag(new TagInputBean("Angelina Jolie", "Person", new EntityTagRelationshipInput("ACTED")));

        movie.merge(brad, angie);
        assertEquals("Tag Inputs did not merge", 3, movie.getTags().size());

        EntityInputBean producer = new EntityInputBean()
            .setCode("tt0356910")
            .setDocumentType(movieDoc)
            .addTag(new TagInputBean("Angelina Jolie", "Person", new EntityTagRelationshipInput("PRODUCED")));

        movie.merge(producer);
        assertEquals("Existing tag with different relationship not recorded", 3, movie.getTags().size());
        TagInputBean angieTag = movie.getTags().get(movie.getTags().indexOf(producer.getTags().iterator().next()));
        assertEquals("An acting and production relationship should exist", 2, angieTag.getEntityTagLinks().size());

    }

    @Test
    public void docTypeInArray() {
        Map<Document, String> docTypes = new HashMap<>();
        Fortress fortress = MockDataFactory.getFortress("Testing", MockDataFactory.getCompany("Testing"));
        Document documentType = MockDataFactory.getDocument(fortress, "Blah");
        docTypes.put(documentType, "OK");
        assertNotNull(docTypes.get(documentType));
    }

    @Test
    public void serialization() throws Exception {
        Fortress fortress = new FortressInputBean("blah", true);
        EntityInputBean bean = new EntityInputBean(fortress, new DocumentTypeInputBean("Testing"));
        bean.setReplaceExistingTags(true);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(bean);

        EntityInputBean other = objectMapper.readValue(bytes, EntityInputBean.class);
        assertNotNull(other);
        assertTrue(other.isReplaceExistingTags());
    }
}
