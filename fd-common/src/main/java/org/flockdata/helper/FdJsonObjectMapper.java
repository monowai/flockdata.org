/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mholdsworth
 * @tag Json, Serialization
 * @since 28/10/2014
 */
public class FdJsonObjectMapper extends JsonFactory {
  private static ObjectMapper objectMapper = null;
  private static Lock l = new ReentrantLock();

  /**
   * @return Jackson 2.0 mapper with comments enabled
   */
  public static ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      try {
        l.lock();
        if (objectMapper == null) {
          objectMapper = new ObjectMapper(new FdJsonObjectMapper());
          //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

      } finally {
        l.unlock();
      }
    }

    return objectMapper;
  }

  @Override
  public JsonParser createParser(URL url) throws IOException {
    JsonParser p = super.createParser(url);
    setParserFeatures(p);
    return p;
  }

  private void setParserFeatures(JsonParser p) {
    p.enable(JsonParser.Feature.ALLOW_COMMENTS);
  }

  @Override
  public JsonParser createParser(InputStream stream) throws IOException {
    JsonParser p = super.createParser(stream);
    setParserFeatures(p);
    return p;
  }

  @Override
  public JsonParser createParser(File file) throws IOException {
    JsonParser p = super.createParser(file);
    setParserFeatures(p);
    return p;
  }
}
