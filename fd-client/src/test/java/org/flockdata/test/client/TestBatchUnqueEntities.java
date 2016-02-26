/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 *
 * Created by mike on 23/01/16.
 */
public class TestBatchUnqueEntities extends AbstractImport{

    /**
     * Given a source with the same entity and different tags, we should be able to batch one entity + many tags
     * rather than wire over one entity+ one tag.
     * @throws Exception
     */
    @Test
    public void duplicateKeysInSource_UniqueEntity() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/duplicate-entities.json";
        ClientConfiguration configuration = getClientConfiguration();
        assertNotNull(configuration);
        configuration.setLoginUser("test");

        ContentProfileImpl contentProfile = ClientConfiguration.getImportProfile(fileName);

        contentProfile.setHeader(true);
        contentProfile.setDocumentName("Movie"); // ToDo: Deserialize DocumentInputBean
        contentProfile.setContentType(ContentProfile.ContentType.CSV);
        contentProfile.setTagOrEntity(ContentProfile.DataType.ENTITY);
        contentProfile.setEntityOnly(true);

        MockFdWriter fdWriter = new MockFdWriter();
        fileProcessor.processFile(contentProfile, "/data/duplicate-entities.csv", fdWriter, null, configuration);
        List<EntityInputBean> entities = fdWriter.getEntities();
        TestCase.assertEquals(1, entities.size());

        EntityInputBean movie = entities.iterator().next();
        int personCount = 0;
        for (TagInputBean tag : movie.getTags()) {
            if ( tag.getLabel().equals("Person"))
                personCount++;
        }

        assertEquals("Should be 2 directors + 3 actors",5, personCount);

    }
}
