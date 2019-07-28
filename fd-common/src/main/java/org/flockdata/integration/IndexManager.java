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

package org.flockdata.integration;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.ArrayUtils;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.Tag;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.QueryParams;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides centralized access to the way that FD handles Entity data for different
 * databases
 *
 * @author mholdsworth
 * @tag Search, Fortress
 * @since 23/07/2015
 */
@Configuration
@Profile( {"fd-server", "dev"})
public class IndexManager {

  private static final String SEPARATOR = ".";
  private Logger logger = LoggerFactory.getLogger("configuration");

  @Value("${org.fd.engine.fortress.index.entity.prefix:fd.}")
  private String prefix;

  @Value("${org.fd.engine.fortress.index.system.prefix:.fd.}")
  // Kind of hidden as this is a system search cache
  private String fdSystemIndexPrefix;

  @Value("${org.fd.search.index.typeSuffix:true}")
  private Boolean typeSuffix;   // use docType as an index suffix?


  IndexManager() {
  }

  public IndexManager(String prefix, boolean typeSuffix) {
    this.prefix = prefix;
    this.typeSuffix = typeSuffix;
  }

  // Determines if the segment is a regular default
  private static boolean isDefaultSegment(String segment) {
    return segment == null || segment.equals(Fortress.DEFAULT);
  }

  @PostConstruct
  void dumpConfig() {
    logger.info("**** Prefixing FD Entity indexes with [{}] and it is [{}] that we will also suffix with the entity type", prefix, typeSuffix);
    logger.info("**** Prefixing FD System indexes with [{}] ", fdSystemIndexPrefix);
  }

  public String getPrefix() {
    return prefix;
  }

  public Boolean isSuffixed() {
    return typeSuffix;
  }

  /**
   * Default way of building an Index. Works for most database types including
   * ElasticSearch
   *
   * @param entity properties
   * @return parsed index
   */
  public String toIndex(Entity entity) {
    if (entity.getSegment().isDefault()) {
      return entity.getFortress().getRootIndex() + getSuffix(entity.getType());
    } else {
      String index = entity.getFortress().getRootIndex() + getSuffix(entity.getType());
      index = index + SEPARATOR + entity.getSegment().getCode().toLowerCase();
      return index;
    }
  }

  public String toStoreIndex(Entity entity) {
    return toIndex(entity);
  }

  /**
   * The suffix, if any, to use for the index. Depends on fd.search.index.typeSuffix==true
   *
   * @param type to analyse
   * @return coded DocumentType
   */
  private String getSuffix(String type) {
    if (isSuffixed() && type != null) {
      return SEPARATOR + parseType(type);
    } else {
      return "";
    }
  }

  public String getTagIndexRoot(Company company, Tag tag) {
    return getTagIndexRoot(company.getCode(), tag.getLabel());
  }

  public String getTagIndexRoot(String company, String label) {
    return getTagIndexRoot(company.toLowerCase()) + SEPARATOR + label.toLowerCase();
  }

  private String getTagIndexRoot(String company) {
    return fdSystemIndexPrefix + company.toLowerCase() + SEPARATOR + "tags";
  }

  /**
   * Root index for an Entity
   *
   * @param fortress     system
   * @param documentType entity type
   * @return FQN
   */
  public String getIndexRoot(Fortress fortress, Document documentType) {
    return getIndexRoot(fortress) + getSuffix(documentType.getName());
  }

  /**
   * Toor index for a fortress
   *
   * @param fortress system
   * @return prefix.company.forterss
   */
  public String getIndexRoot(Fortress fortress) {

    if (fortress.isSystem()) {
      return getIndexRoot(fdSystemIndexPrefix, fortress.getCompany().getCode(), fortress.getCode());
    }
    return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
  }

  private String getIndexRoot(String company, String fortress) {
    return getIndexRoot(getPrefix(), company, fortress);
  }

  /**
   * @param prefix   user defined prefix
   * @param company  arbitrary company
   * @param fortress arbitrary fortress
   * @return absolute name of the index
   */
  private String getIndexRoot(String prefix, String company, String fortress) {
    String fort = (fortress == null || fortress.equals("*") ? "" : SEPARATOR + fortress.toLowerCase());
    return prefix + company.toLowerCase() + fort;
  }

  /**
   * @param queryParams used to compute the result
   * @return computed index name based on queryParams
   */
  public String toIndex(QueryParams queryParams) {
    String index;
    if (queryParams.isSearchTagsOnly()) {
      index = getTagIndexRoot(queryParams.getCompany()) + SEPARATOR + "*";
    } else {
      // Entity index root
      String indexRoot = getIndexRoot(queryParams.getCompany(), queryParams.getFortress());
      if (isDefaultSegment(queryParams.getSegment())) {
        return indexRoot;
      }
      index = String.format("%s.%s", indexRoot, queryParams.getSegment());
    }
    logger.debug("Resolved {} index to {}", queryParams, index);
    return index;
  }

  /**
   * Computes ES indexes, including wildcards, from the supplied query parameters
   *
   * @param queryParams Args holding parameters to use in the query
   * @return one index per doc type
   * @see QueryParams
   */
  public String[] getIndices(QueryParams queryParams) {
    if (queryParams.getIndex() != null) {
      if (queryParams.getTypes() == null) {
        return new String[] {queryParams.getIndex() + "*"};
      }

      return ArrayUtils.toArray(queryParams.getIndex());
    }
    return getIndices(queryParams.getCompany(), queryParams.getFortress(), queryParams.getTypes(), queryParams.getSegment());
  }

  /**
   * prefix.company.fortress.type.segment
   *
   * @param company  owns the index
   * @param fortress owns the index data
   * @param types    types to scan
   * @param segment  optional segment to restrict by
   * @return One index line per Root+Type combination
   */
  public String[] getIndices(String company, String fortress, String[] types, String segment) {
    Collection<String> results = new ArrayList<>();
    if (company == null && fortress == null && types == null && segment == null) {
      results.add(getPrefix() + "*");
      return results.toArray(new String[0]);
    }

    if (fortress == null && types == null && segment == null) {
      results.add(getPrefix() + company.toLowerCase() + SEPARATOR + "*");
      return results.toArray(new String[0]);
    }

    String indexPath = getPrefix() + (company != null ? company.toLowerCase() : "*");
    String segmentFilter;

    if (segment != null && !isDefaultSegment(segment)) {
      segmentFilter = SEPARATOR + segment.toLowerCase();
    } else {
      segmentFilter = "*";// all segments
    }

    String fortressFilter;
    if (fortress == null || fortress.equals("*")) {
      fortressFilter = SEPARATOR + "*";
    } else {
      fortressFilter = (segmentFilter.equals("") ? SEPARATOR + fortress.toLowerCase() + SEPARATOR + "*" : SEPARATOR + fortress.toLowerCase());
    }

    indexPath = indexPath + fortressFilter;

    if (types == null || types.length == 0) {
      results.add(indexPath + segmentFilter);
    } else {
      for (String type : types) { //ToDo filtering by type not supported

        if (!results.contains(indexPath)) {
          String typeFilter;
          if (type == null) {
            typeFilter = "";
          } else {
            typeFilter = SEPARATOR + type.toLowerCase();
          }
          results.add(indexPath + typeFilter + segmentFilter);
        }
      }
    }

    return results.toArray(new String[0]);
  }

  public String parseType(Entity entity) {
    return parseType(entity.getType());
  }

  public String parseType(String type) {
    return type.toLowerCase();
  }

  public String resolveKey(LogRequest logRequest) throws NotFoundException {
    if (logRequest.getStore() == Store.NONE) {
      // ElasticSearch
      if (logRequest.getEntity().getSearchKey() == null)
      //throw new NotFoundException("Unable to resolve the search key for the entity " + logRequest.getEntity().toString());
      {
        return logRequest.getEntity().getKey();
      } else {
        return logRequest.getEntity().getSearchKey();
      }
    }
    return logRequest.getLogId().toString();

  }

  public String toStoreIndex(Store store, Entity entity) {
    if (store == null) {
      return toIndex(entity);
    }
    return toStoreIndex(entity);
  }

  public String toStoreIndex(StoredContent storedContent) {
    return toIndex(storedContent.getEntity());
  }

}

