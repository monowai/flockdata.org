/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.transform.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.data.ContentModel;
import org.flockdata.transform.json.ExtractProfileDeserializer;

/**
 * @author mholdsworth
 * @since 28/01/2016
 */
@JsonDeserialize(using = ExtractProfileDeserializer.class)
public interface ExtractProfile {

  ContentType getContentType();

  ExtractProfile setContentType(ContentType contentType);

  char getDelimiter();

  ExtractProfile setDelimiter(String delimiter);

  Boolean hasHeader();

  String getHandler();

  String getPreParseRowExp();

  void setPreParseRowExp(String expression);

  String getQuoteCharacter();

  ExtractProfile setQuoteCharacter(String quoteCharacter);

  ExtractProfile setHeader(boolean header);

  ContentModel getContentModel();

  enum ContentType {CSV, JSON, XML}
}
