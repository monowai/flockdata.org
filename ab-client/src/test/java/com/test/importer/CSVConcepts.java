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

package com.test.importer;

import com.auditbucket.client.Importer;
import com.auditbucket.client.common.CsvTagMapper;
import com.auditbucket.client.common.ImportParams;
import com.auditbucket.registration.bean.TagInputBean;

import java.util.Collection;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: mike
 * Date: 20/06/14
 * Time: 10:19 AM
 */
public class CSVConcepts {
    @org.junit.Test
    public void csvTags() throws Exception{
        ImportParams params = Importer.getImportParams("/csv-tag-import.json", null);
        CsvTagMapper mapper = new CsvTagMapper(params);
        String[] headers= new String[]{"company_name",     "device_name",     "type", "city"};
        String[] data = new String[]{  "Samsoon", "Palaxy", "Mobile Phone", "Auckland"};
        Map<String,Object> json = mapper.setData(headers, data, params);
        assertNotNull (json);
        Map<String, Collection<TagInputBean>> allTargets = mapper.getTargets();
        assertNotNull(allTargets);
        assertEquals(2, allTargets.size());
        assertEquals("Should have overridden the column name of device_name", "Device", mapper.getIndex());
        assertEquals("Palaxy", mapper.getCode());

        assertEquals(2, allTargets.size());

        TagInputBean makes = allTargets.get("makes").iterator().next();
        assertEquals("Manufacturer", makes.getIndex());
        assertEquals("Nested City tag not found", 1, makes.getTargets().size());
        TagInputBean city = makes.getTargets().get("located").iterator().next();
        assertEquals("Auckland", city.getName());


        assertEquals("Samsoon", makes.getCode());
        assertEquals("Should be using the column name", "type", allTargets.get("of-type").iterator().next().getIndex());

    }
}
