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

import java.util.Map;
import org.joda.time.DateTime;

/**
 * Container representing a uniquely identifiable piece of data
 *
 * @author mike
 * @tag Entity
 * @since 1/01/17
 */
public interface Entity {
  /**
   * @return FD internally unique ID
   */
  Long getId();

  /**
   * @return GUID
   */
  String getKey();

  String getName();

  /**
   * @return lower case type of the entity
   */
  String getType();

  FortressUser getLastUser();

  Long getLastUpdate();

  FortressUser getCreatedBy();

  /**
   * @return user defined properties to be recorded against the entity
   */
  Map<String, Object> getProperties();

  /**
   * @return is indexing this entity in the search system suppressed
   */
  boolean isSearchSuppressed();

  /**
   * @return unique identifier within the search service
   */
  String getSearchKey();

  /**
   * @return callers unique identifier for this type of entity
   */
  String getCode();

  long getDateCreated();

  /**
   * @return nicely formatted date and time based on when this was created in the owning fortress (fortressCreate)
   */
  DateTime getFortressCreatedTz();

  /**
   * @return is this entity new to FlockData?
   */
  boolean isNewEntity();

  /**
   * @return true if there are no logs for this entity
   */
  boolean isNoLogs();

  /**
   * @return computer system that owns this entity
   */
  Fortress getFortress();

  /**
   * @return the segment that this entity exist in
   */
  Segment getSegment();

  /**
   * @return last known event to occur against this entity
   */
  String getEvent();

  /**
   * Flags the entity as having been affected by search. Can be used for testing when waiting
   * for a count to be increased
   *
   * @return current search count
   */
  Integer getSearch();

  /**
   * Convenience method
   *
   * @return return Updated date in the Timezone of the Fortress
   */
  DateTime getFortressUpdatedTz();
}
