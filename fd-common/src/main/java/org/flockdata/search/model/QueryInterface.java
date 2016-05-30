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

import java.util.ArrayList;

/**
 * Created by mike on 4/05/15.
 */
public interface QueryInterface {

    String getCompany();

    // Saves parsing for ElasticSearch
    String[] getTypes();

    ArrayList<String> getRelationships();

    ArrayList<String> getTags();

    String getSearchText();

    // This request is only to be made on tags
    boolean isSearchTagsOnly();
}
