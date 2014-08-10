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

package com.auditbucket.test.unit;

import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.LogInputBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * User: Mike Holdsworth
 * Since: 18/07/13
 */
public class TestJson {

    @Test
    public void testUtf8Compression () throws Exception{
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        CompressionResult dataBlock = CompressionHelper.compress(json);

        String uncompressed = CompressionHelper.decompress(dataBlock);
        Assert.assertEquals(uncompressed, json);

    }
    @Test
    public void compressLotsOfBytes() throws Exception {
        Map<String,Object> json = TestHelper.getBigJsonText(99);
        //System.out.println("Pretty JSON          - " + json.getBytes("UTF-8").length);
        LogInputBean log = new LogInputBean("", "", null, json);
        //System.out.println("JSON Node (unpretty) - " + log.getWhat().);

        CompressionResult result = CompressionHelper.compress(json);
        System.out.println("Compress Pretty      - " + result.length());
        result = CompressionHelper.compress(log.getWhat());
        System.out.println("Compressed JSON      - " + result.length());

        Assert.assertEquals(CompressionResult.Method.GZIP, result.getMethod());

        json = TestHelper.getBigJsonText(99);
        String uncompressed = CompressionHelper.decompress(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode compareTo = mapper.valueToTree(json);
        JsonNode other = mapper.readTree(uncompressed);
        Assert.assertTrue(compareTo.equals(other));
    }

    @Test
    public void simpleTextRemainsUncompressed() throws Exception {
        String json = "{\"colname\": \"tinytext.......................\"}";
        System.out.println("Before Comppression" + json.getBytes("UTF-8").length);

        CompressionResult result = CompressionHelper.compress(json);
        Assert.assertEquals(CompressionResult.Method.NONE, result.getMethod());
        System.out.println("Compressed " + result.length());

        String uncompressed = CompressionHelper.decompress(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode compareTo = mapper.readTree(json);
        JsonNode other = mapper.readTree(uncompressed);
        Assert.assertTrue(compareTo.equals(other));

    }
    @Test
    public void diffUtils() throws Exception{
        String jsonA = "{\"house\": \"red\", \"bedrooms\": 2, \"list\": [3,2,1]}";
        String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map mapA, mapB;
        ObjectMapper mapper = new ObjectMapper();

        mapA = mapper.readValue(jsonA, HashMap.class);
        mapB = mapper.readValue(jsonB, HashMap.class);
        MapDifference diff = Maps.difference(mapA, mapB);
        System.out.print(diff.entriesOnlyOnLeft());
    }

}
