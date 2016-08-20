/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.integration;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.search.model.QueryParams;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides centralized access to the way that FD handles Entity data for different
 * databases
 * <p/>
 * Created by mike on 23/07/15.
 */
@Configuration
public class IndexManager {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${org.fd.engine.fortress.index.entity.prefix:fd.}")
    private String prefix;

    @Value("${org.fd.engine.fortress.index.system.prefix:.fd.}") // Kind of hidden as this is a system search cache
    private String fdSystemIndexPrefix;

    @Value("${org.fd.search.index.typeSuffix:true}")
    private Boolean typeSuffix;   // use docType as an index suffix?


    IndexManager() {
    }

    public IndexManager(String prefix, boolean typeSuffix) {
        this.prefix = prefix;
        this.typeSuffix = typeSuffix;
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

    public String parseIndex(Entity entity) {
        return parseIndex(entity, entity.getType());
    }

    /**
     * Default way of building an Index. Works for most database types including
     * ElasticSearch
     *
     * @param entity properties
     * @return parsed index
     */
    public String parseIndex(Entity entity, String type) {
        if (entity.getSegment().isDefault())
            return entity.getSegment().getFortress().getRootIndex() + getSuffix(type);
        else {
            String index = entity.getSegment().getFortress().getRootIndex() + getSuffix(type);
            index = index + "." + entity.getSegment().getCode().toLowerCase();
            return index;
        }
    }

    public String toStoreIndex(Entity entity) {
        return parseIndex(entity);
    }

    /**
     * The suffix, if any, to use for the index. Depends on fd.search.index.typeSuffix==true
     *
     * @param type to analyse
     * @return coded DocumentType
     */
    private String getSuffix(String type) {
        if (isSuffixed() && type != null)
            return "." + parseType(type);
        else
            return "";
    }

    public String getIndexRoot(Company company, Tag tag) {
        return getTagIndexRoot(company) + getSuffix(tag.getLabel().toLowerCase());
    }

    private String getTagIndexRoot(Company company) {
        return getTagIndexRoot(company.getCode());
    }

    private String getTagIndexRoot(String company) {
        return fdSystemIndexPrefix + company.toLowerCase() + ".tags";
    }

    public String getIndexRoot(Fortress fortress, DocumentType documentType) {
        return getIndexRoot(fortress)+getSuffix(documentType.getName());
    }

    public String getIndexRoot(Fortress fortress) {

        if (fortress.isSystem()) {
            return getIndexRoot(fdSystemIndexPrefix, fortress.getCompany().getCode(), fortress.getCode());
        }
        return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
    }

    private String getIndexRoot(String company, String fortress) {
        return getIndexRoot(getPrefix(), company, fortress);
    }

    // Returns the root level of the index with no doctype or consideration of a segment
    private String getIndexRoot(String prefix, String company, String fortress) {
        String fort = (fortress == null || fortress.equals("*") ? "" : "." + fortress.toLowerCase());
        return prefix + company.toLowerCase() + fort;
    }

    public String parseIndex(QueryParams queryParams) {
        String index;
        if (queryParams.isSearchTagsOnly()) {
            index = getTagIndexRoot(queryParams.getCompany());
        } else {
            // Entity index root
            String indexRoot = getIndexRoot(queryParams.getCompany(), queryParams.getFortress());
            if (isDefaultSegment(queryParams.getSegment()))
                return indexRoot;
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
     * @throws FlockException
     */
    public String[] getIndexesToQuery(QueryParams queryParams) throws FlockException {
        if (queryParams.getIndex() != null) {
            if (queryParams.getTypes() == null)
                return new String[]{queryParams.getIndex() + ".*"};
            String indexes[] = new String[queryParams.getTypes().length];
            int i = 0;
            for (String type : queryParams.getTypes()) {
                indexes[i] = queryParams.getIndex() + "." + parseType(type);
            }
            return indexes;
        }
        return getIndexesToQuery(queryParams.getCompany(), queryParams.getFortress(), queryParams.getTypes(), queryParams.getSegment());
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
    public String[] getIndexesToQuery(String company, String fortress, String[] types, String segment) {

        Collection<String> results = new ArrayList<>();

        String indexPath = getPrefix() + (company != null ? company.toLowerCase() : "*");
        String segmentFilter = "";

        if (segment != null && !isDefaultSegment(segment)) {
            segmentFilter = "." + segment.toLowerCase();
        } else {
            segmentFilter = "*";// all segments
        }

        String fortressFilter;
        if (fortress == null || fortress.equals("*"))
            fortressFilter = ".*";
        else
            fortressFilter = (segmentFilter.equals("") ? "." + fortress.toLowerCase() + ".*" : "." + fortress.toLowerCase());

        indexPath = indexPath + fortressFilter;

        if (types == null || types.length == 0) {
            results.add(indexPath + segmentFilter);
        } else {
            for (String type : types) { //ToDo filtering by type not supported

                if (!results.contains(indexPath)) {
                    String typeFilter;
                    if (type == null)
                        typeFilter = "";
                    else
                        typeFilter = "." + type.toLowerCase();
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

    // Determines if the segment is a regular default
    private static boolean isDefaultSegment(String segment) {
        return segment == null || segment.equals(FortressSegment.DEFAULT);
    }

    public String resolveKey(LogRequest logRequest) throws NotFoundException {
        if (logRequest.getStore() == Store.NONE) {
            // ElasticSearch
            if (logRequest.getEntity().getSearchKey() == null)
                //throw new NotFoundException("Unable to resolve the search key for the entity " + logRequest.getEntity().toString());
                return logRequest.getEntity().getKey();
            else
                return logRequest.getEntity().getSearchKey();
        }
        return logRequest.getLogId().toString();

    }

    public String toStoreIndex(Store store, Entity entity) {
        if (store == null)
            return parseIndex(entity);
        return toStoreIndex(entity);
    }

    public String toStoreIndex(StoredContent storedContent) {
        return parseIndex(storedContent.getEntity(), storedContent.getType());
    }
}

