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
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.json.JsonEntityMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 10/12/14
 * Time: 1:43 PM
 */
public class TestJsonEntity {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(TestCsvEntity.class);

    FdReader reader = new FdReader() {
        @Override
        public String resolveCountryISOFromName(String name) throws FlockException {
            return name;
        }

        @Override
        public String resolve(String type, Map<String, Object> args) {
            return null;
        }
    };

    @Test
    public void entity_JsonStructure() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/gov.json");
        JsonEntityMapper entity = new JsonEntityMapper();

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream file = TestCsvEntity.class.getResourceAsStream("/object-example.json");
            JsonNode theMap = mapper.readTree(file);
            entity.setData(theMap, params, reader);
            assertEquals(11, entity.getTags().size());
            assertEquals("hr4015-113", entity.getCallerRef());
            assertEquals("hr", entity.getDocumentType());
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
        FileProcessor fileProcessor = new FileProcessor(reader);
        ImportProfile profile = ClientConfiguration.getImportParams("/gov.json");
        profile.setContentType(ProfileConfiguration.ContentType.JSON);
        profile.setTagOrEntity(ProfileConfiguration.DataType.ENTITY);
        profile.setFortressName("testing");

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        ClientConfiguration defaults = new ClientConfiguration();
        fileProcessor.processFile(profile, "/object-example.json", 0, writer,company, defaults  );
    }

    @Test
    public void array_ImportJsonEntities() throws Exception{
        FileProcessor fileProcessor = new FileProcessor(reader);
        ImportProfile profile = ClientConfiguration.getImportParams("/gov.json");
        profile.setContentType(ProfileConfiguration.ContentType.JSON);
        profile.setFortressName("testing");
        profile.setTagOrEntity(ProfileConfiguration.DataType.ENTITY);

        Company company = Mockito.mock(Company.class);
        company.setName("Testing");
        ClientConfiguration defaults = new ClientConfiguration();
        fileProcessor.processFile(profile, "/array-example.json", 0, writer,company, defaults  );
    }


    FdWriter writer = new FdWriter() {
        @Override
        public SystemUserResultBean me() {
            return null;
        }

        @Override
        public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            ObjectMapper om = new ObjectMapper();
            try {
                om.writeValueAsString(tagInputBeans);
            } catch (Exception e) {
                logger.error("Unexpected", e);
                throw new FlockException("Failed to serialize");
            }
            return null;
        }

        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            ObjectMapper om = new ObjectMapper();
            assertEquals(1, entityBatch.size());
            assertNotNull(entityBatch.iterator().next().getLog());
            try {
                om.writeValueAsString(entityBatch);
            } catch (Exception e) {
                logger.error("Unexpected", e);
                throw new FlockException("Failed to serialize");
            }
            return null;
        }

        @Override
        public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
            return 0;
        }

        @Override
        public boolean isSimulateOnly() {
            return false;
        }

        @Override
        public Collection<Tag> getCountries() throws FlockException {
            return null;
        }

        @Override
        public void close() {

        }
    };
}
