package org.flockdata.test.helper;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Verifies that EntityInputBeans can be merged into each other
 *
 * Created by mike on 23/01/16.
 */
public class TestEntityInputBean {

    @Test
    public void testMerge() throws Exception {
        DocumentTypeInputBean movieDoc = new DocumentTypeInputBean("Movie");
        EntityInputBean movie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Doug Liman", "Person", "DIRECTED"));


        EntityInputBean brad = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Brad Pitt", "Person", "ACTED"));

        EntityInputBean angie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Angelina Jolie", "Person", "ACTED"));

        movie.merge(brad,angie);
        assertEquals("Tag Inputs did not merge", 3, movie.getTags().size());

        EntityInputBean producer = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Angelina Jolie", "Person", "PRODUCED"));

        movie.merge(producer);
        assertEquals("Existing tag with different relationship not recorded", 3, movie.getTags().size());
        TagInputBean angieTag = movie.getTags().get(movie.getTags().indexOf(producer.getTags().iterator().next()));
        assertEquals ("An acting and production relationship should exist", 2, angieTag.getEntityLinks().size());

    }


}
