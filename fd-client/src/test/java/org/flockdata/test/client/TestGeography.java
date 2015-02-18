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
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.csv.CsvTagMapper;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * SPeL tests and custom properties for tags
 *
 * Created by mike on 17/01/15.
 */
public class TestGeography {

    @Test
    public void string_Countries() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        // CsvImporter will convert Lon/Lat to doubles - ToDo: write CSV Import tests
        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(headers, data, params);
        assertNotNull(tag);

        assertEquals("NZ", tag.getCode() );
        assertEquals("New Zealand", tag.getName() );
        assertEquals("Custom properties not being set", 2, tag.getProperties().size());
        boolean latitudeSet = false, longitudeSet = false;
        for (String key : tag.getProperties().keySet()) {
            if ( key.equals("latitude")) {
                latitudeSet = true;
            } else if ( key.equals("longitude")){
                longitudeSet = true;
            }
        }

        assertEquals("Latitude value not found",true, latitudeSet);
        assertEquals("Longitude value not found", true, longitudeSet);

    }

    @Test
    public void string_ConditionalTag() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(headers, data, params);
        assertNotNull(tag);

        assertEquals("Capital city was not present", 1, tag.getTargets().size());

        data = new String[]{"NZ","New Zealand","-41.27","174.71","0", "Wellington" };
        tag = new CsvTagMapper(); // Clear down the object
        tag.setData(headers, data, params);
        assertEquals("Capital city was not suppressed", 0, tag.getTargets().size());
    }

    @Test
    public void string_ConditionalTagProperties() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(headers, data, params);
        assertNotNull(tag);

        assertEquals("Capital city was not present", 1, tag.getTargets().size());
        Collection<TagInputBean> capitals = tag.getTargets().get("capital");
        for (TagInputBean next : capitals) {
            assertEquals(2, next.getProperties().size());
        }

    }


}
