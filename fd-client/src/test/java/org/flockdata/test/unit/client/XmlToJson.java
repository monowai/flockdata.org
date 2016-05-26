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

package org.flockdata.test.unit.client;

import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 22/09/14
 * Time: 2:36 PM
 */
public class XmlToJson {
    @Test
    public void blah(){
        JSONObject jsonObject = XML.toJSONObject(getXml());

        assertEquals ("Walnes", jsonObject.getJSONObject("person").get("lastname"));
    }

    public String getXml (){
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
