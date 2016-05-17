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

package org.flockdata.search.model;

/**
 * User: Mike Holdsworth
 * Since: 5/09/13
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
    public static final String DOC_TYPE = "docType";
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
    public static final String PROPS = "udp";
    public static final String INDEX = "index";

    /**
     *
     * @param types unparsed docTypes
     * @return lowercase doc types
     */
    public static String[] parseDocTypes(String[] types) {
        String[] results = new String[types.length];
        int i = 0;
        for (String type : types) {
            results[i]= type.toLowerCase();
        }
        return results;
    }
}
