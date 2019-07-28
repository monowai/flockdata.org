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

package org.flockdata.data;


/**
 * FD organises system data into sub-documents
 * <p>
 * Each Entity is classified as being of a "DocumentType"
 * <p>
 * For example, Invoice, Customer, Person etc.
 *
 * @author mholdsworth
 * @tag DocumentType
 * @since 30/06/2013
 */

public interface Document {
  String getName();

  String getGeoQuery();

  Boolean isSearchEnabled();

  Boolean isStoreEnabled();

  Boolean isTrackEnabled();

  EntityTag.TAG_STRUCTURE getTagStructure();

  Document.VERSION getVersionStrategy();

  String getCode();

  Long getId();

  Fortress getFortress();

  /**
   * Set the version strategy on a per DocumentType basis
   * <p>
   * Enable version control when segment.storeEnabled== false
   * Suppress when your segment.storeEnabled== true and you don't want to version
   * Fortress (default) means use whatever the segment default is
   */
  enum VERSION {
    FORTRESS, ENABLE, DISABLE
  }
}
