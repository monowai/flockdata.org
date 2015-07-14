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

import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestTagAliases extends AbstractImport {
    @Test
    public void string_csvTagAliases() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String paramFile = "/profile/csv-tag-alias.json";
        ClientConfiguration configuration = getClientConfiguration(paramFile);

        ImportProfile params = ClientConfiguration.getImportParams(paramFile);
        fileProcessor.processFile(params, "/data/csv-tag-alias.txt", getFdWriter(), null, configuration);

        Collection<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(3, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            switch (tagInputBean.getCode()) {
                case "AL":
                    assertTrue(tagInputBean.hasAliases());
                    assertNotNull(tagInputBean.getNotFoundCode());
                    assertEquals(1, tagInputBean.getAliases().size());
                    assertEquals("1", tagInputBean.getAliases().iterator().next().getCode());
                    assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                    break;
                case "AK":
                    assertTrue(tagInputBean.hasAliases());
                    assertNotNull(tagInputBean.getNotFoundCode());
                    assertEquals(1, tagInputBean.getAliases().size());
                    assertEquals("2", tagInputBean.getAliases().iterator().next().getCode());
                    assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                    break;
                case "AB":
                    assertFalse(tagInputBean.hasAliases());
                    break;
            }
        }
    }



}
