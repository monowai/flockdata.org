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

import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 1/03/15.
 */
public class TestLabels extends AbstractImport {
    @Test
    public void conflict_LabelDefinition() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/tag-labels.json");
        FileProcessor fileProcessor = new FileProcessor();

        fileProcessor.processFile(ProfileReader.getImportProfile("/tag-labels.json"),
                "/tag-labels.csv", getFdWriter(), null, configuration);

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
        fileProcessor.processFile(ProfileReader.getImportProfile("/tag-label-expressions.json"),
                "/tag-label-expressions.csv", getFdWriter(), null, configuration);

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
                assertEquals("011", tagInputBean.getCode());
            else
                throw new FlockException("Unexpected tag - " + tagInputBean.toString());

        }
    }

    @Test
    public void alias_DescriptionEvaluates() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/profile/labels.json");
        FileProcessor fileProcessor = new FileProcessor();
        fileProcessor.processFile(ProfileReader.getImportProfile("/profile/labels.json"),
                "/data/assets.txt", getFdWriter(), null, configuration);
        List<EntityInputBean>entities = getFdWriter().getEntities();
        List<TagInputBean> tagInputBeans = entities.iterator().next().getTags();
        assertEquals(1, tagInputBeans.size());
        TestCase.assertEquals(3, entities.iterator().next().getTags().iterator().next().getAliases().size());
        List<TagInputBean> tags = entities.iterator().next().getTags();
        for (TagInputBean tag : tags) {
            Collection<AliasInputBean> aliase = tag.getAliases();
            for (AliasInputBean alias : aliase) {
                switch (alias.getDescription()){
                    case "ISIN":
                    case "Asset PK":
                    case "assetCode":
                        break;
                    default:
                        throw new Exception("Unexpected alias description " + alias.toString());
                }
            }
        }
    }

}
