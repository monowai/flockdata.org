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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVTagsWithDelimiter extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/no-header.json");
        ClientConfiguration configuration = Configure.readConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ClientConfiguration.getImportParams("/no-header.json");
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/no-header.txt", getFdWriter(), null, configuration);
        int expectedRows = 6;
        assertEquals(expectedRows, rows);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        TestCase.assertEquals(expectedRows, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            assertFalse(tagInputBean.getCode().contains("|"));
            assertFalse(tagInputBean.getName().contains("|"));
            TestCase.assertEquals(1, tagInputBean.getTargets().size());
            assertFalse("non-persistent mapping was not ignored", tagInputBean.hasTagProperties());
            Collection<TagInputBean> targets = tagInputBean.getTargets().get("represents");
            for (TagInputBean represents : targets ) {
                assertFalse(represents.getCode().contains("|"));
                assertTrue(represents.isMustExist());

            }
        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(tagInputBeans);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }


}
