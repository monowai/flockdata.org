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

package org.flockdata.search;

/**
 * @author mholdsworth
 * @since 5/09/2013
 */
public class SearchSchema {
  // Storage schema used in a Search Document
  public static final String DATA = "data";
  public static final String KEY = "key";
  public static final String CODE = "code";
  public static final String DESCRIPTION = "description";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String FORTRESS = "fortress";
  public static final String DOC_TYPE = "_type";
  public static final String ATTACHMENT = "attachment";

  public static final String TAG = "tag";
  public static final String ALL_TAGS = "tags";
  public static final String LAST_EVENT = "lastEvent";
  public static final String WHO = "who";
  public static final String CREATED = "whenCreated"; // Date the document was first created in the Fortress
  public static final String UPDATED = "whenUpdated";

  public static final String WHAT_CODE = "code";
  public static final String WHAT_NAME = "name";
  public static final String WHAT_DESCRIPTION = "description";
  public static final String FILENAME = "filename";
  public static final String CONTENT_TYPE = "contentType";
  public static final String PROPS = "up";
  public static final String INDEX = "index";
  public static final String ENTITY = "e";
  public static final String TAG_PROPS = "properties";
  public static final String ENTITY_FIELD = "e.";
  public static final String TAG_UDP = "." + TAG_PROPS;
  public static final String DATA_FIELD = "data.";
  public static final String FACET_FIELD = ".facet";
  public static final String TAG_FIELD = TAG + ".";

  /**
   * @param types unparsed docTypes
   * @return lowercase doc types
   */
  public static String[] parseDocTypes(String[] types) {
    String[] results = new String[types.length];
    int i = 0;
    for (String type : types) {
      results[i] = type.toLowerCase();
    }
    return results;
  }
}
