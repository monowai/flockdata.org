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

import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

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
        String paramFile = "/csv-tag-alias.json";
        ClientConfiguration configuration = getClientConfiguration(paramFile);

        ImportProfile params = ClientConfiguration.getImportParams(paramFile);
        fileProcessor.processFile(params, "/csv-tag-alias.txt", 0, fdWriter, null, configuration);

    }

    static FdWriter fdWriter = new FdWriter() {
        @Override
        public SystemUserResultBean me() {
            return null;
        }

        @Override
        public  String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            // One row has no us-census column but should still be in the result set
            assertEquals(3, tagInputBeans.size());
            for (TagInputBean tagInputBean : tagInputBeans) {
                switch (tagInputBean.getCode()) {
                    case "AL":
                        assertTrue(tagInputBean.hasAliases());
                        assertEquals(1, tagInputBean.getAliases().size());
                        assertEquals("1", tagInputBean.getAliases().iterator().next().getCode());
                        assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                        break;
                    case "AK":
                        assertTrue(tagInputBean.hasAliases());
                        assertEquals(1, tagInputBean.getAliases().size());
                        assertEquals("2", tagInputBean.getAliases().iterator().next().getCode());
                        assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                        break;
                    case "AB":
                        assertFalse(tagInputBean.hasAliases());
                        break;
                }
            }
            return null;
        }



        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            return null;
        }

        @Override
        public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
            return 0;
        }

        @Override
        public boolean isSimulateOnly() {
            // Setting this to true will mean that the flush routines above are not called
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
