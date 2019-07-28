/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.helper.CompressionResult;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author mholdsworth
 * @since 18/07/2013
 */
public class TestCompression {

  @Test
  public void compressed_Utf8() {
    String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
    CompressionResult dataBlock = ObjectHelper.compress(json);

    String uncompressed = ObjectHelper.decompress(dataBlock);
    Assert.assertEquals(uncompressed, json);

  }

  @Test
  public void compressed_bytesAreSquashed() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(99);
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

    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    JsonNode compareTo = mapper.valueToTree(content);
    JsonNode other = mapper.readTree(uncompressed);
    Assert.assertEquals(compareTo, other);
  }


  @Test
  public void uncompressed_notCompressed() throws Exception {
    String json = "{\"colname\": \"tinytext.......................\"}";
    System.out.println("Before Compression" + json.getBytes(StandardCharsets.UTF_8).length);

    CompressionResult result = ObjectHelper.compress(json);
    Assert.assertEquals(CompressionResult.Method.NONE, result.getMethod());
    System.out.println("Compressed " + result.length());

    String uncompressed = ObjectHelper.decompress(result);

    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    JsonNode compareTo = mapper.readTree(json);
    JsonNode other = mapper.readTree(uncompressed);
    Assert.assertEquals(compareTo, other);
  }

  @Test
  public void json_diff() throws Exception {
    String jsonA = "{\"house\": \"red\", \"bedrooms\": 2, \"list\": [3,2,1]}";
    String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";
    Map mapA, mapB;
    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

    mapA = mapper.readValue(jsonA, HashMap.class);
    mapB = mapper.readValue(jsonB, HashMap.class);
    MapDifference diff = Maps.difference(mapA, mapB);
    System.out.print(diff.entriesOnlyOnLeft());
  }

  @Test
  public void compressionDisabled() {
    ContentInputBean content = new ContentInputBean("mike", new DateTime());
    content.setAttachment(ContentDataHelper.getPdfDoc(), "pdf", "test.pdf");
    System.setProperty(ObjectHelper.PROP_COMPRESSION, "true");
    CompressionResult result = ObjectHelper.compress(content);
    assertEquals(result.getMethod(), CompressionResult.Method.NONE);
  }

}
