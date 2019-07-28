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

package org.flockdata.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.ContentModel;

/**
 * Payload required to validate Content with a selection of data
 *
 * @author mholdsworth
 * @since 14/04/2016
 */
public class ContentValidationRequest {

  ContentModel contentModel;
  Collection<Map<String, Object>> rows;
  Map<Integer, Collection<String>> messages = new HashMap<>();

  public ContentValidationRequest() {
  }

  public ContentValidationRequest(ContentModel model) {
    this();
    this.contentModel = model;
  }

  public ContentValidationRequest(Map<String, Object> dataMap) {
    Collection<Map<String, Object>> rows = new ArrayList<>();
    rows.add(dataMap);
    this.rows = rows;
  }

  public ContentValidationRequest(ContentModel contentModel, Collection<Map<String, Object>> data) {
    this(contentModel);
    this.rows = data;
  }

  public ContentValidationRequest(Collection<Map<String, Object>> rows) {
    this.rows = rows;
  }

  public ContentModel getContentModel() {
    return contentModel;
  }

  public ContentValidationRequest setContentModel(ContentModel contentModel) {
    this.contentModel = contentModel;
    return this;
  }

  public Collection<Map<String, Object>> getRows() {
    return rows;
  }

  public Map<Integer, Collection<String>> getMessages() {
    return messages;
  }

  public void addResult(int rowCount, String message) {

    Collection<String> existingMessages = messages.get(rowCount);
    if (existingMessages == null) {
      existingMessages = new ArrayList<>();
      messages.put(rowCount, existingMessages);
    }
    existingMessages.add(message);
  }
}
