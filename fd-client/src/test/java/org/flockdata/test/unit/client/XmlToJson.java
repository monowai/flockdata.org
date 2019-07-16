/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.test.unit.client;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 22/09/2014
 */
public class XmlToJson {
    @Test
    public void blah() {
        JSONObject jsonObject = XML.toJSONObject(getXml());

        assertEquals("Walnes", jsonObject.getJSONObject("person").get("lastname"));
    }

    public String getXml() {
        return "<person>\n" +
            "  <firstname>Joe</firstname>\n" +
            "  <lastname>Walnes</lastname>\n" +
            "  <phone>\n" +
            "    <code>123</code>\n" +
            "    <number>1234-456</number>\n" +
            "  </phone>\n" +
            "  <fax>\n" +
            "    <code>123</code>\n" +
            "    <number>9999-999</number>\n" +
            "  </fax>\n" +
            "</person>";
    }


}
