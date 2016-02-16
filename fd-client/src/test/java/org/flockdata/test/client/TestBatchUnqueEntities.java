package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.client.Configure;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.io.File;
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
        File file = new File(fileName);
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

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
