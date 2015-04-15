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
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 27/02/15.
 */
public class TestDataTypes extends AbstractImport {
    @Test
    public void preserve_NumberValueAsString() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ClientConfiguration configuration = getClientConfiguration(fileName);

        ImportProfile params = ClientConfiguration.getImportParams(fileName);
        fileProcessor.processFile(params, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(1, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if (tagInputBean.getLabel().equals("as-string"))
                assertEquals("00165", tagInputBean.getCode());
        }
        EntityInputBean entity = getFdWriter().getEntities().iterator().next();
        assertNotNull ( entity.getContent());
        assertEquals("The N/A string should have been set to the default of 0", 0, entity.getContent().getWhat().get("illegal-num"));
        assertEquals("The Blank string should have been set to the default of 0", 0, entity.getContent().getWhat().get("blank-num"));
    }

    @Test
    public void preserve_TagCodeAlwaysString() throws Exception {
        // Even though the source column can be treated as a number, it should be set as a String because
        // it drives a tag code.
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ClientConfiguration configuration = getClientConfiguration(fileName);

        ImportProfile params = ClientConfiguration.getImportParams(fileName);
        fileProcessor.processFile(params, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(2, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if (tagInputBean.getLabel().equals("tag-code"))
                assertEquals("123", tagInputBean.getCode());
        }
        List<EntityInputBean> entities = getFdWriter().getEntities();
        for (EntityInputBean entity : entities) {
            Object whatString = entity.getContent().getWhat().get("tag-code");
            assertEquals(""+whatString.getClass(), true, whatString instanceof String);
        }

    }






}