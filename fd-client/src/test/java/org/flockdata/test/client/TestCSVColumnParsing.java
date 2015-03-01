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

import org.flockdata.client.Configure;
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

import java.io.File;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVColumnParsing {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/column-parsing.json");
        ClientConfiguration configuration = Configure.readConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ClientConfiguration.getImportParams("/profile/column-parsing.json");
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/pac.txt", 0, fdWriter, null, configuration);
        assertEquals(1l, rows);

    }

    FdWriter fdWriter = new FdWriter() {
        @Override
        public SystemUserResultBean me() {
            return null;
        }

        @Override
        public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            assertEquals(4, tagInputBeans.size());
            boolean foundA = false, foundB= false,foundC= false, foundD= false;
            for (TagInputBean tagInputBean : tagInputBeans) {
                if ( tagInputBean.getLabel().equals("OSCategory")){
                    foundA = true;
                    assertEquals("E1140", tagInputBean.getCode());
                } else if ( tagInputBean.getLabel().equals("Expenditure")){
                    foundB = true;
                    assertEquals("D", tagInputBean.getCode());
                    assertEquals("Direct", tagInputBean.getName());
                }  else if ( tagInputBean.getLabel().equals("InterestGroup")){
                    foundC = true;
                    assertEquals("C00485250", tagInputBean.getCode());
                }  else if ( tagInputBean.getLabel().equals("Politician")){
                    foundD = true;
                    assertEquals("N00031647", tagInputBean.getCode());
                }

            }
            assertTrue("Failed to find OS Category Tag", foundA);
            assertTrue("Failed to find Expenditure Tag", foundB);
            assertTrue("Failed to find InterestGroup Tag", foundC);
            assertTrue("Failed to find Politician Tag", foundD);
            return null;
        }

        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            for (EntityInputBean entityInputBean : entityBatch) {
                assertEquals("4111320141231324700", entityInputBean.getCallerRef());
            }
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
