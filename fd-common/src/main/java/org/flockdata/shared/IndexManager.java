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

package org.flockdata.shared;

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
    private String fdSystemIndexPrefix ;

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

    public String getIndexRoot(Fortress fortress) {

        if ( fortress.isSystem()){
            return getIndexRoot(fdSystemIndexPrefix,fortress.getCompany().getCode(), fortress.getCode());
        }
        return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
    }

    private String getIndexRoot(String company, String fortress){
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
        }else {
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
        if (queryParams.getIndex() !=null ){
            if ( queryParams.getTypes() == null )
                return new String[]{queryParams.getIndex()+".*"};
            String indexes[] = new String[queryParams.getTypes().length];
            int i=0;
            for (String type : queryParams.getTypes()) {
                indexes[i]= queryParams.getIndex()+"."+parseType(type);
            }
            return indexes;
        }
        return getIndexesToQuery(queryParams.getCompany(), queryParams.getFortress(), queryParams.getSegment(), queryParams.getTypes());
    }

    /**
     * @param company  owns the index
     * @param fortress owns the index data
     * @param segment  optional segment to restrict by
     * @param types    types to scan
     * @return One index line per Root+Type combination
     */
    public String[] getIndexesToQuery(String company, String fortress, String segment, String[] types) {
        int length = 1;
        if (types != null && types.length > 0)
            length = 1;
        String[] results = new String[length];
        Collection<String> found = new ArrayList<>();

        String indexRoot = getPrefix() + (company != null ? company.toLowerCase() : "*");
        String segmentFilter = "";

        if (segment != null) {
            if (!isDefaultSegment(segment))
                segmentFilter = "." + segment.toLowerCase();
        }

        String fortressFilter;
        if (fortress == null || fortress.equals("*"))
            fortressFilter = ".*";
        else
            fortressFilter = (segmentFilter.equals("") ? "." + fortress.toLowerCase() + "*" : "." + fortress.toLowerCase());

        indexRoot = indexRoot + fortressFilter + segmentFilter;

        if (types == null || types.length == 0) {
            results[0] = indexRoot;
        } else {
            int count = 0;
            for (String type : types) {
                if (!found.contains(indexRoot)) {
                    results[count] = indexRoot; //+ "."+type.toLowerCase();
                    found.add(indexRoot);
                    count++;
                }
            }
        }

        return results;
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

