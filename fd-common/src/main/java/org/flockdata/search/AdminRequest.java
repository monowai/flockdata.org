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

package org.flockdata.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Encapsulate as yet not totally defined functionality.
 * Currently supports deleting collection of indexes between fd-engine and fd-search
 *
 * @author mholdsworth
 * @since 12/05/2016
 */
//@JsonDeserialize(using = AdminRequestDeserializer.class)
public class AdminRequest {

  private Collection<String> indexesToDelete;

  @SuppressWarnings("WeakerAccess")
  public AdminRequest() {
  }

  public AdminRequest(String indexToDelete) {
    this();
    setIndexToDelete(indexToDelete);
  }

  public Collection<String> getIndexesToDelete() {
    return indexesToDelete;
  }

  public void setIndexesToDelete(Collection<String> delete) {
    this.indexesToDelete = delete;
  }

  @JsonIgnore
  private void setIndexToDelete(String indexToDelete) {
    indexesToDelete = new ArrayList<>();
    indexesToDelete.add(indexToDelete);

  }

  public void addIndexToDelete(String searchIndexToDelete) {
    if (this.indexesToDelete == null) {
      indexesToDelete = new ArrayList<>();
    }
    indexesToDelete.add(searchIndexToDelete);
  }
}
