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

package org.flockdata.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityResultBean;

/**
 * @author mholdsworth
 * @since 17/09/2014
 */
@JsonDeserialize(as = StorageBean.class)
public interface StoredContent {

  String getAttachment();

  Map<String, Object> getData();

  String getChecksum();

  /**
   * @return primary key for this content
   */
  Object getId();

  ContentInputBean getContent();

  String getStore();

  void setStore(String store);

  String getType();

  EntityResultBean getEntity();
}
