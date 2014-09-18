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

import com.auditbucket.engine.repo.KvContentData;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.model.KvContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * User: Mike Holdsworth
 * Since: 18/07/13
 */
public class TestCompression {

    @Test
    public void compressed_Utf8() throws Exception{
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        CompressionResult dataBlock = CompressionHelper.compress(json);

        String uncompressed = CompressionHelper.decompress(dataBlock);
        Assert.assertEquals(uncompressed, json);

    }
    @Test
    public void compressed_bytesAreSquashed() throws Exception {
        Map<String,Object> json = Helper.getBigJsonText(99);
        //System.out.println("Pretty JSON          - " + json.getBytes("UTF-8").length);
        KvContent content = new KvContentData(json);
        //System.out.println("JSON Node (unpretty) - " + log.getLogInputBean().);

        CompressionResult result = CompressionHelper.compress(content);
        System.out.println("Compress Pretty      - " + result.length());
        result = CompressionHelper.compress(content);
        System.out.println("Compressed JSON      - " + result.length());

        Assert.assertEquals(CompressionResult.Method.GZIP, result.getMethod());

        //json = TestHelper.getBigJsonText(99);
        String uncompressed = CompressionHelper.decompress(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode compareTo = mapper.valueToTree(content);
        JsonNode other = mapper.readTree(uncompressed);
        Assert.assertTrue(compareTo.equals(other));
    }

    @Test
    public void checksum_Constant() throws Exception{
        ContentInputBean content = new ContentInputBean("mike", new DateTime());
        content.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        KvContent kvContent = new KvContentData(content);
        CompressionResult result = CompressionHelper.compress(kvContent);
        final String checksum = result.getChecksum();
        assertNotNull(checksum);


        result = CompressionHelper.compress(kvContent);
        assertTrue("re-compressing should yield same checksum", checksum.equals(result.getChecksum()));

        content.setAttachment(Helper.getPdfDoc() + "a", "pdf", "test.pdf");
        kvContent = new KvContentData(content);
        result = CompressionHelper.compress(kvContent);
        assertFalse(checksum.equals(result.getChecksum()));

        content.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        kvContent = new KvContentData(content);
        result = CompressionHelper.compress(kvContent);
        assertTrue("re-compressing should yield same checksum", checksum.equals(result.getChecksum()));
    }

    @Test
    public void uncompressed_notCompressed() throws Exception {
        String json = "{\"colname\": \"tinytext.......................\"}";
        System.out.println("Before Compression" + json.getBytes("UTF-8").length);

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
    public void json_diff() throws Exception{
        String jsonA = "{\"house\": \"red\", \"bedrooms\": 2, \"list\": [3,2,1]}";
        String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
        Map mapA, mapB;
        ObjectMapper mapper = new ObjectMapper();

        mapA = mapper.readValue(jsonA, HashMap.class);
        mapB = mapper.readValue(jsonB, HashMap.class);
        MapDifference diff = Maps.difference(mapA, mapB);
        System.out.print(diff.entriesOnlyOnLeft());
    }

    @Test
    public void compressionDisabled(){
        ContentInputBean content = new ContentInputBean("mike", new DateTime());
        content.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        KvContent kvContent = new KvContentData(content);
        System.setProperty(CompressionHelper.PROP_COMPRESSION, "true");
        CompressionResult result = CompressionHelper.compress(kvContent);
        assertTrue(result.getMethod().equals(CompressionResult.Method.NONE));


    }

}
