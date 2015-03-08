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

package org.flockdata.test.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.helper.CompressionResult;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;


/**
 * User: Mike Holdsworth
 * Since: 18/07/13
 */
public class TestCompression {

    @Test
    public void compressed_Utf8() throws Exception{
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        CompressionResult dataBlock = ObjectHelper.compress(json);

        String uncompressed = ObjectHelper.decompress(dataBlock);
        Assert.assertEquals(uncompressed, json);

    }
    @Test
    public void compressed_bytesAreSquashed() throws Exception {
        Map<String,Object> json = Helper.getBigJsonText(99);
        //System.out.println("Pretty JSON          - " + json.getBytes("UTF-8").length);
        //ContentInputBean content = ;
//        KvContent content = new KvContentBean(json);
        //System.out.println("JSON Node (unpretty) - " + log.getLogInputBean().);
        ContentInputBean content = new ContentInputBean(json);
        CompressionResult result = ObjectHelper.compress(content);
        System.out.println("Compress Pretty      - " + result.length());
        result = ObjectHelper.compress(content);
        System.out.println("Compressed JSON      - " + result.length());

        Assert.assertEquals(CompressionResult.Method.GZIP, result.getMethod());

        //json = TestHelper.getBigJsonText(99);
        String uncompressed = ObjectHelper.decompress(result);

        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        JsonNode compareTo = mapper.valueToTree(content);
        JsonNode other = mapper.readTree(uncompressed);
        Assert.assertTrue(compareTo.equals(other));
    }


    @Test
    public void uncompressed_notCompressed() throws Exception {
        String json = "{\"colname\": \"tinytext.......................\"}";
        System.out.println("Before Compression" + json.getBytes("UTF-8").length);

        CompressionResult result = ObjectHelper.compress(json);
        Assert.assertEquals(CompressionResult.Method.NONE, result.getMethod());
        System.out.println("Compressed " + result.length());

        String uncompressed = ObjectHelper.decompress(result);

        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        JsonNode compareTo = mapper.readTree(json);
        JsonNode other = mapper.readTree(uncompressed);
        Assert.assertTrue(compareTo.equals(other));

    }
    @Test
    public void json_diff() throws Exception{
        String jsonA = "{\"house\": \"red\", \"bedrooms\": 2, \"list\": [3,2,1]}";
        String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map mapA, mapB;
        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();

        mapA = mapper.readValue(jsonA, HashMap.class);
        mapB = mapper.readValue(jsonB, HashMap.class);
        MapDifference diff = Maps.difference(mapA, mapB);
        System.out.print(diff.entriesOnlyOnLeft());
    }

    @Test
    public void compressionDisabled(){
        ContentInputBean content = new ContentInputBean("mike", new DateTime());
        content.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        System.setProperty(ObjectHelper.PROP_COMPRESSION, "true");
        CompressionResult result = ObjectHelper.compress(content);
        assertTrue(result.getMethod().equals(CompressionResult.Method.NONE));

    }

}
