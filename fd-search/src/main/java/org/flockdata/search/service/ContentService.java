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

package org.flockdata.search.service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.EsColumn;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.configure.SearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Structure
 * @since 31/08/2016
 */
@Service
public class ContentService {

  private final IndexManager indexManager;
  private final SearchConfig searchConfig;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public ContentService(SearchConfig searchConfig) {
    this.searchConfig = searchConfig;
    this.indexManager = searchConfig.getIndexManager();
  }


  public ContentStructure getStructure(QueryParams queryParams) throws FlockException {
    try {
      String[] indexes = indexManager.getIndices(queryParams);
      GetFieldMappingsRequestBuilder fieldMappings = searchConfig.getClient()
          .admin().indices().prepareGetFieldMappings(indexes);
      fieldMappings.setFields("data.*", "tag.*", "e.*", "up.*", SearchSchema.DESCRIPTION,
          SearchSchema.PROPS, SearchSchema.CREATED, SearchSchema.UPDATED, SearchSchema.DOC_TYPE);
      ListenableActionFuture<GetFieldMappingsResponse> future = fieldMappings.execute();
      GetFieldMappingsResponse result = future.get();
      Map<String, Map<String, Map<String,
          GetFieldMappingsResponse.FieldMappingMetaData>>> mappings = result.mappings();

      for (String index : mappings.keySet()) {
        Map<String, Map<String,
            GetFieldMappingsResponse.FieldMappingMetaData>> stringImmutableMapImmutableMap = mappings.get(index);

        for (String type : stringImmutableMapImmutableMap.keySet()) {
          ContentStructure contentStructure = new ContentStructure(index, type);
          Map<String, GetFieldMappingsResponse.FieldMappingMetaData> fields = stringImmutableMapImmutableMap.get(type);
          for (String field : fields.keySet()) {
            handle(contentStructure, fields.get(field));
          }
          return contentStructure;
        }
      }
//            logger.info(result.toString());
      return null;
    } catch (InterruptedException | ExecutionException e) {
      logger.error(e.getMessage());
      throw new FlockException(e.getMessage());
    }
  }

  private void handle(ContentStructure contentStructure, GetFieldMappingsResponse.FieldMappingMetaData fieldMappingMetaData) {
    String name = fieldMappingMetaData.fullName();
    boolean link = false, fd = false, data = false;
    if (name.startsWith("e.") || name.startsWith("tag.")) {
      link = true;
    } else if (name.startsWith("data.")) {
      data = true;
    } else if (name.startsWith("up.")) {
      data = true;
    } else {
      fd = true;
    }

    EsColumn column = null;
    if (!fd) {
      Map<String, Object> source = fieldMappingMetaData.sourceAsMap();
      if (link && isKeyword(source)) {
        column = new EsColumn(name, "string");
      } else {
        Collection<Object> values = fieldMappingMetaData.sourceAsMap().values();
        for (Object key : values) {
          Map<String, Object> props = (Map<String, Object>) key;
          if (props.containsKey("type")) {
            String dataType = props.get("type").toString();
            if (dataType.equals("date") || dataType.equals("long") || dataType.equals("number") || dataType.equals("double")) {
              column = new EsColumn(name, dataType);
            }
          }

        }
      }
    } else {
      if (name.startsWith("when")) {
        column = new EsColumn(name, "date");
      } else {
        column = new EsColumn(name, "string");
      }
    }
    if (column != null) {
      if (link) {
        contentStructure.addLink(column);
      } else if (data) {
        contentStructure.addData(column);
      } else {
        contentStructure.addFd(column);
      }
    }
  }

  private boolean isKeyword(Map<String, Object> source) {
    if (source.containsKey("keyword")) {
      return true;
    }
    Map<String, Object> defn = (Map<String, Object>) source.values().iterator().next();
    if (defn.containsKey("type") && defn.get("type").equals("keyword")) {
      return true;
    }
    if (defn.containsKey("fields")) {
      Map<String, Object> candidate = (Map<String, Object>) defn.get("fields");
      return candidate.containsKey("keyword");
    }
    return false;
  }
}
