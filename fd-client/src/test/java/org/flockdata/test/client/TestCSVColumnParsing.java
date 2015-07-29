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
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVColumnParsing extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/column-parsing.json");
        ClientConfiguration configuration = Configure.readConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ClientConfiguration.getImportParams("/profile/column-parsing.json");
        assertEquals(false, params.hasHeader());

        long rows = fileProcessor.processFile(params, "/data/pac.txt", getFdWriter(), null, configuration);
        assertEquals(1l, rows);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertNotNull ( tagInputBeans);
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
        for (EntityInputBean entityInputBean : getFdWriter().getEntities()) {
            assertEquals("4111320141231324700", entityInputBean.getCode());
        }
    }



}
