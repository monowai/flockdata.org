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
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by mike on 1/03/15.
 */
public class TestLabels extends AbstractImport {
    @Test
    public void conflict_LabelDefinition() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/tag-labels.json");
        FileProcessor fileProcessor = new FileProcessor();

        fileProcessor.processFile(ClientConfiguration.getImportParams("/tag-labels.json"),
                "/tag-labels.csv", 0, getFdWriter(), null, configuration);

        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(2, tagInputBeans.size());
        boolean loanType=false, occupancy= false;
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("Occupancy")) {
                occupancy = true;
                assertEquals("1", tagInputBean.getCode());
                assertEquals(null, tagInputBean.getName());
            }else if ( tagInputBean.getLabel().equals("Loan Type")) {
                loanType = true;
                assertEquals("blah", tagInputBean.getCode());
                assertEquals(null, tagInputBean.getName());
            } else
                throw new FlockException("Unexpected tag - " + tagInputBean.toString());

        }
        assertTrue("Occupancy Not Found", occupancy);
        assertTrue("loanType Not Found", loanType);
    }

    @Test
    public void label_expressionsAndConstants() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/tag-label-expressions.json");
        FileProcessor fileProcessor = new FileProcessor();
        fileProcessor.processFile(ClientConfiguration.getImportParams("/tag-label-expressions.json"),
                "/tag-label-expressions.csv", 0, getFdWriter(), null, configuration);

        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        // 1 Politician
        //
        assertEquals(4, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("Agency"))
                assertEquals("1", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("Edit Status"))
                assertEquals("7", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("MSA/MD"))
                assertEquals("10180", tagInputBean.getCode());
            else if ( tagInputBean.getLabel().equals("County"))
                assertEquals("9", tagInputBean.getCode());
            else
                throw new FlockException("Unexpected tag - " + tagInputBean.toString());

        }
    }






}
