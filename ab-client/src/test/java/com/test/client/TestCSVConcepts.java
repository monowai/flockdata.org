/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.test.client;

import com.auditbucket.client.ClientConfiguration;
import com.auditbucket.client.rest.FdRestReader;
import com.auditbucket.profile.ImportProfile;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.transform.csv.CsvTagMapper;

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
        ImportProfile params = ClientConfiguration.getImportParams("/csv-tag-import.json");
        CsvTagMapper mappedTag = new CsvTagMapper();
        String[] headers= new String[]{"company_name", "device_name",  "device_code", "type",         "city", "ram", "tags"};
        String[] data = new String[]{  "Samsoon",      "Palaxy",       "PX",          "Mobile Phone", "Auckland", "32mb", "phone,thing,other"};
        FdRestReader fdRestReader = new FdRestReader(null);
        Map<String,Object> json = mappedTag.setData(headers, data, params, fdRestReader);
        assertNotNull (json);
        Map<String, Collection<TagInputBean>> allTargets = mappedTag.getTargets();
        assertNotNull(allTargets);
        assertEquals(3, allTargets.size());
        assertEquals("Should have overridden the column name of device_name", "Device", mappedTag.getLabel());
        assertEquals("Palaxy", mappedTag.getName());
        assertEquals("PX", mappedTag.getCode());
        assertEquals("Device", mappedTag.getLabel());
        assertNotNull(mappedTag.getProperties().get("RAM"));

        TagInputBean makes = allTargets.get("makes").iterator().next();
        assertEquals("Manufacturer", makes.getLabel());
        assertEquals("Nested City tag not found", 1, makes.getTargets().size());
        TagInputBean city = makes.getTargets().get("located").iterator().next();
        assertEquals("Auckland", city.getName());


        assertEquals("Samsoon", makes.getCode());
        assertEquals("Should be using the column name", "type", allTargets.get("of-type").iterator().next().getLabel());
        assertEquals(3, allTargets.get("mentions").size());

    }
}
