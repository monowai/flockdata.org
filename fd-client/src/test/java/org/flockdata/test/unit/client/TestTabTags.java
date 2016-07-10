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

package org.flockdata.test.unit.client;

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.tags.TagMapper;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tag targets are handled
 * Created by mike on 27/01/15.
 */
public class TestTabTags {
    @Test
    public void string_NestedTags() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/sectors.json");
        TagMapper mapper = new TagMapper();
        String[] headers = new String[]{"Catcode","Catname","Catorder","Industry","Sector","Sector Long"};
        String[] data = new String[]{"F2600","Private Equity & Investment Firms","F07","Securities & Investment","Finance/Insur/RealEst","Finance","Insurance & Real Estate"};

        Map<String, Object> json = mapper.setData(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)),params);
        assertNotNull(json);
        assertNotNull(mapper);
        assertEquals("Code does not match", "F2600", mapper.getCode());
        assertEquals("Name does not match", "Private Equity & Investment Firms", mapper.getName());
        assertNotNull(mapper.getProperties().get("order"));
        assertEquals(1, mapper.getTargets().size());
    }

}