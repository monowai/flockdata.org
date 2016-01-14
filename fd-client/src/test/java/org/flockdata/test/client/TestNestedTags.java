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

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 1/03/15.
 */
public class TestNestedTags extends AbstractImport {

    @Test
    public void label_missingColumnDoesNotCreateTargetTag() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/profile/interest-groups.json");
        FileProcessor fileProcessor = new FileProcessor();
        fileProcessor.processFile(ProfileReader.getImportProfile("/profile/interest-groups.json"),
                "/data/tags-inputs.csv", getFdWriter(), null, configuration);

        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        // The profile defines a nested tag but the value is missing in the source

        assertEquals(1, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            System.out.println(tagInputBean);
            assertFalse("The target tag should not exist as the source value was missing", tagInputBean.hasTargets());
        }
    }
}
