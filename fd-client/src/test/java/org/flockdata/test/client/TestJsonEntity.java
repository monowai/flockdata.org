/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.model.Company;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.flockdata.transform.json.JsonEntityMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 10/12/14
 * Time: 1:43 PM
 */
public class TestJsonEntity extends AbstractImport{

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(TestCsvEntity.class);

    @Test
    public void entity_JsonStructure() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/gov.json");
        JsonEntityMapper entity = new JsonEntityMapper();

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream file = TestCsvEntity.class.getResourceAsStream("/profile/object-example.json");
            JsonNode theMap = mapper.readTree(file);
            entity.setData(theMap, params);
            assertEquals(11, entity.getTags().size());
            assertEquals("hr4015-113", entity.getCode());
            assertEquals("hr", entity.getDocumentType().getName());
            assertNotNull(entity.getName());
            for (TagInputBean tagInputBean : entity.getTags()) {
                if ( tagInputBean.getEntityLinks().get("sponsors")!=null ) {
                    assertNotNull(tagInputBean.getTargets().get("located"));
                    Collection<TagInputBean> states = tagInputBean.getTargets().get("located");
                    for (TagInputBean state : states) {
                        assertEquals("TX", state.getCode());
                        assertNotNull(state.getTargets().get("represents"));
                    }
                }

                else if ( tagInputBean.getEntityLinks().get("cosponsors")!=null )
                    //assertEquals(5,  );
                    logger.info("Validate CoSponsor {}", tagInputBean.toString());

            }
        } catch (IOException e) {

            logger.error("Error writing exceptions", e);
            throw new RuntimeException("IO Exception ", e);
        }



    }

    @Test
    public void object_ImportJsonEntity() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        ContentProfileImpl profile = ProfileReader.getImportProfile("/profile/gov.json");
        profile.setContentType(ContentProfile.ContentType.JSON);
        profile.setTagOrEntity(ContentProfile.DataType.ENTITY);
        profile.setFortressName("testing");

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        ClientConfiguration defaults = new ClientConfiguration();
        long rows = fileProcessor.processFile(profile, "/profile/object-example.json", getFdWriter(),company, defaults  );
        assertEquals("Should have processed the file as a single JSON object", 1, rows);
    }

    @Test
    public void array_ImportJsonEntities() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        ContentProfileImpl profile = ProfileReader.getImportProfile("/profile/gov.json");
        profile.setContentType(ContentProfile.ContentType.JSON);
        profile.setFortressName("testing");
        profile.setTagOrEntity(ContentProfile.DataType.ENTITY);

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        ClientConfiguration defaults = new ClientConfiguration();
        long rows = fileProcessor.processFile(profile, "/profile/array-example.json", getFdWriter(),company, defaults  );
        assertEquals("Should have processed the file as an array of JSON objects", 1, rows);
    }



}
