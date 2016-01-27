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

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ProfileReader;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.csv.CsvTagMapper;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 20/06/14
 * Time: 10:19 AM
 */
public class TestCSVConcepts {
    @org.junit.Test
    public void csvTags() throws Exception{
        ContentProfileImpl params = ProfileReader.getImportProfile("/csv-tag-import.json");
        CsvTagMapper mappedTag = new CsvTagMapper();
        String[] headers= new String[]{"company_name", "device_name",  "device_code", "type",         "city", "ram", "tags"};
        String[] data = new String[]{  "Samsoon",      "Palaxy",       "PX",          "Mobile Phone", "Auckland", "32mb", "phone,thing,other"};

        Map<String,Object> json = mappedTag.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull (json);
        Map<String, Collection<TagInputBean>> allTargets = mappedTag.getTargets();
        assertNotNull(allTargets);
        assertEquals(3, allTargets.size());
        assertEquals("Should have overridden the column name of device_name", "Device", mappedTag.getLabel());
        assertEquals("Name value should be that of the defined column", "Palaxy", mappedTag.getName());
        assertEquals("PX", mappedTag.getCode());
        assertEquals("Device", mappedTag.getLabel());
        assertNotNull(mappedTag.getProperties().get("RAM"));

        TagInputBean makes = allTargets.get("makes").iterator().next();
        assertEquals("Manufacturer", makes.getLabel());
        assertEquals("Nested City tag not found", 1, makes.getTargets().size());
        TagInputBean city = makes.getTargets().get("located").iterator().next();
        assertEquals("Auckland", city.getCode());


        assertEquals("Samsoon", makes.getCode());
        assertEquals("Should be using the column name", "type", allTargets.get("of-type").iterator().next().getLabel());
        assertEquals(3, allTargets.get("mentions").size());

    }
}
