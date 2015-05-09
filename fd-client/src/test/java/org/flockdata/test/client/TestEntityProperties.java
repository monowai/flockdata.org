/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by mike on 6/05/15.
 */
public class TestEntityProperties extends AbstractImport {
    @Test
    public void process_individualData() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/entity-properties.json";
        File file = new File(fileName);
        ClientConfiguration configuration = Configure.readConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ClientConfiguration.getImportParams(fileName);
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        fileProcessor.processFile(params, "/data/entity-properties.txt", getFdWriter(), null, configuration);
        List<EntityInputBean> entities = getFdWriter().getEntities();

        for (EntityInputBean entity : entities) {
            assertNotEquals("One org and one candidate", 0, entity.getTags().size());
            for (TagInputBean tagInputBean : entity.getTags()) {
                switch ( tagInputBean.getLabel()) {
                    case "Year":
                        assertEquals("2014", tagInputBean.getCode());
                        break;
                    case "Contributor":
                        assertEquals("j10013521891", tagInputBean.getCode());
                        assertNotNull(tagInputBean.getName());
                        assertNotSame(tagInputBean.getName(), tagInputBean.getCode());
                        break;
                    case "OSCategory":
                        assertEquals("G6400", tagInputBean.getCode());
                        break;
                    case "Politician":
                        break;
                    case "ZipCode":
                        assertEquals("Zip code should not be turned to a number. Should be preserved as a string", "07450", tagInputBean.getCode());

                        assertNotNull(tagInputBean.getTargets().get("located"));
                        TagInputBean city = tagInputBean.getTargets().get("located").iterator().next();
                        assertNotNull ( city);
                        assertNotNull(city.getTargets().get("city"));
                        TagInputBean state = city.getTargets().get("city").iterator().next();
                        assertEquals("US-NJ", state.getCode());
                        break;
                    default:
                        throw new Exception("Unexpected tag "+tagInputBean);

                }
            }
//            TagInputBean contributor = entity.getTags().get("contributed");
            assertNotNull(entity.getProperties());
            assertTrue (entity.getProperties().get("value")!=null );
            Object value = entity.getProperties().get("value");
            assertTrue(value instanceof Number);

        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entities);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }


    }
}
