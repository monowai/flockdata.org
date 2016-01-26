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

import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.model.Tag;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.*;
import org.flockdata.transform.csv.CsvTagMapper;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * SPeL tests and custom properties for tags
 *
 * Created by mike on 17/01/15.
 */
public class TestGeography extends AbstractImport{

    private Logger logger = getLogger(TestGeography.class);

    @Test
    public void string_Countries() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        // CsvImporter will convert Lon/Lat to doubles - ToDo: write CSV Import tests
        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(tag);

        assertEquals("NZ", tag.getCode() );
        assertEquals("New Zealand", tag.getName() );
        assertEquals("Custom properties not being set", 2, tag.getProperties().size());
        boolean latitudeSet = false, longitudeSet = false;
        for (String key : tag.getProperties().keySet()) {
            if ( key.equals("latitude")) {
                latitudeSet = true;
                assertEquals(-41.27f, tag.getProperties().get("latitude"));
            } else if ( key.equals("longitude")){
                assertEquals(174.71f, tag.getProperties().get("longitude"));
                longitudeSet = true;
            }
        }

        assertEquals("Latitude value not found",true, latitudeSet);
        assertEquals("Longitude value not found", true, longitudeSet);

    }

    @Test
    public void string_ConditionalTag() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(tag);

        assertEquals("Capital city was not present", 1, tag.getTargets().size());

        data = new String[]{"NZ","New Zealand","-41.27","174.71","0", "Wellington" };
        tag = new CsvTagMapper(); // Clear down the object
        tag.setData(Transformer.convertToMap(headers, data, params), params);
        TestCase.assertFalse("Capital city was not suppressed", tag.hasTargets());
    }

    @Test
    public void string_ConditionalTagProperties() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        String[] data = new String[]{"NZ","New Zealand","-41.27","174.71","1", "Wellington" };

        tag.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(tag);

        assertEquals("Capital city was not present", 1, tag.getTargets().size());
        Collection<TagInputBean> capitals = tag.getTargets().get("capital");
        for (TagInputBean next : capitals) {
            assertEquals(2, next.getProperties().size());
        }

    }

    @Test
    public void null_PropertyValuesNotSaved() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/test-countries.json");
        CsvTagMapper tag = new CsvTagMapper();

        // We will purposefully suppress the capital city to test the conditional expressions
        String[] headers = new String[]{"ISO3166A2","ISOen_name","UNc_latitude","UNc_longitude", "HasCapital", "BGN_capital"};

        String[] data = new String[]{"NZ","New Zealand",null,null,"1", "Wellington" };

        tag.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(tag);

        assertEquals("Capital city was not present", 1, tag.getTargets().size());
        Collection<TagInputBean> capitals = tag.getTargets().get("capital");
        for (TagInputBean next : capitals) {
            TestCase.assertFalse(next.hasTagProperties());
        }

    }

    /**
     * FD uses GeoTools for GIS mapping. Here we are converting an arbitary address in NZ
     * from a NZTM format to to the more popular WGS84
     *
     * @throws Exception
     */
    @Test
    public void geoTools() throws Exception {
        // http://epsg.io/2193
        // Goes in as Lat Lon
        double [] coords = GeoSupport.convert(new GeoPayload("EPSG:2193", 1762370.616143, 5437327.768345));

        //geoDataBean.
        TestCase.assertTrue(coords[1] < -40d);
        TestCase.assertTrue(coords[0] > 170d);
        // Output an example link
        // http://stackoverflow.com/questions/2660201/what-parameters-should-i-use-in-a-google-maps-url-to-go-to-a-lat-lon
        // Lon Lat
        // Most sites use lon/lat
        logger.info("http://maps.google.com/maps?z=12&t=m&q=loc:{}+{}", coords[1], coords[0]);
    }

    @Test
    public void setPropertiesFromSource () throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/import-geo.json";
        File file = new File(fileName);
        ClientConfiguration configuration = Configure.getConfiguration(file);
        TestCase.assertNotNull(configuration);

        ContentProfileImpl params = ProfileReader.getImportProfile(fileName);
        TestCase.assertEquals('|', params.getDelimiter());
        TestCase.assertEquals(true, params.hasHeader());
        TestCase.assertNotNull(params.getCondition());

        fileProcessor.processFile(params, "/data/import-geo.txt", getFdWriter(), null, configuration);

        List<TagInputBean> tags = getFdWriter().getTags();
        assertEquals("Condition expression did not evaluate", 1, tags.size());

        TagInputBean tag = tags.iterator().next();
        assertEquals(2, tag.getProperties().size());
        logger.info("http://maps.google.com/maps?z=12&t=m&q=loc:{}+{}", tag.getProperty(Tag.LON), tag.getProperty(Tag.LAT));
        // Check that geo properties are set on nested tags
        TagInputBean mesh = tag.getTargets().get("mesh").iterator().next();
        assertEquals("Geo properties not set in to nested tag", 2, mesh.getProperties().size());
        assertEquals(tag.getProperty(Tag.LON),mesh.getProperty(Tag.LON));
        assertEquals(tag.getProperty(Tag.LAT),mesh.getProperty(Tag.LAT));

    }


}
