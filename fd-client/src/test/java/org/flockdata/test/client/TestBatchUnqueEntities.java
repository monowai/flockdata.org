/*
 *  Copyright 2012-2016 the original author or authors.
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
