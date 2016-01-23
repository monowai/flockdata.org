/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:33 PM
 */
public class FdLoader {
    private List<EntityInputBean> entityBatch = new ArrayList<>();
    private Map<String, TagInputBean> tagBatch = new HashMap<>();
    private final Lock entityLock = new ReentrantLock();
    private final String tagSync = "TagSync";
    private Company company = null;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FdLoader.class);

    private ClientConfiguration  clientConfiguration;
    FdWriter fdWriter;

    public FdLoader(FdWriter writer, ClientConfiguration configuration) {
        this(writer, configuration, null);

    }

    public FdLoader(FdWriter writer, ClientConfiguration configuration, Company company) {
        this.clientConfiguration = configuration;
        this.fdWriter = writer;
        this.company = company;
        logger.info("Configuration {}", clientConfiguration);

    }

    public void batchTag(TagInputBean tagInputBean, String message) throws FlockException {
        batchTag(tagInputBean, false, message);
    }

    public void batchEntity(EntityInputBean entityInputBean) throws FlockException {
        batchEntity(entityInputBean, false);
    }

    public void batchEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException {

        try {
            entityLock.lock();
            if (entityInputBean != null) {
//                if (entityInputBean.getFortress() == null && importProfile!= null)
//                    entityInputBean.setFortress(importProfile.getFortressName());
                if ( entityInputBean.getFortress() == null || entityInputBean.getFortress().equals(""))
                    throw new FlockException("Unable to resolve the fortress name that owns this entity. Add this via your import profile with the fortressName attribute.");

                if ( entityInputBean.getDocumentName() == null ||  entityInputBean.getDocumentName().equals("") )
                    throw new FlockException("Unable to resolve the document type name that defines this entity. Add this via your import profile with the documentName attribute.");

                int existingIndex = getExistingIndex(entityInputBean);

                if ( existingIndex>-1){
                    EntityInputBean masterEntity = entityBatch.get(existingIndex);
                    masterEntity.merge(entityInputBean);
                } else {
                    entityBatch.add(entityInputBean);
                }
                batchTags(entityInputBean);
            }

            if ( clientConfiguration.getBatchSize()> 0 && (flush || entityBatch.size() >=  clientConfiguration.getBatchSize())) {

                if (entityBatch.size() >0 ) {
                    logger.debug("Flushing....");
                    // process the tags independently to reduce the chance of a deadlock when processing the entity
//                    fdWriter.flushTags(new ArrayList<>(tagBatch.values()));
                    fdWriter.flushEntities(company, entityBatch,  clientConfiguration );
                    logger.debug("Flushed Batch [{}]", entityBatch.size());
                }
                entityBatch = new ArrayList<>();
                tagBatch = new HashMap<>();
            }

        } finally {
            entityLock.unlock();
        }

    }

    /**
     * determines if an entity already being tracked can be considered to be merged with
     * @param entityInputBean incoming entity
     * @return index of an existing EIB or -1 if it should be ignored
     */
    private int getExistingIndex(EntityInputBean entityInputBean) {
        int existingIndex =-1;
        if ( (entityInputBean.getCode()!= null || entityInputBean.getMetaKey()!=null ) && entityInputBean.getContent() == null )
            existingIndex = entityBatch.indexOf(entityInputBean);
        return existingIndex;
    }

    public int getEntityCount(){
        return entityBatch.size();
    }

    private void batchTag(TagInputBean tagInputBean, boolean flush, String message) throws FlockException {

        synchronized (tagSync) {
            if (tagInputBean != null) {
                if ( tagInputBean.getCode()==null || tagInputBean.getCode().equals("")) {
                    logger.error("Attempting to create a tag without a code value. Code is a required field []" + tagInputBean);
                    return ;
                }

                tagBatch.put(getTagKey(tagInputBean), tagInputBean);

            }

            if ( tagBatch.size() >0 )
                if (flush || tagBatch.size() >= clientConfiguration.getBatchSize()) {
                    logger.debug("Flushing " + message + " Tag Batch [{}]", tagBatch.size());
                    if (tagBatch.size() > 0)
                        fdWriter.flushTags(new ArrayList<>(tagBatch.values()));
                    logger.debug("Tag Batch Flushed");
                    tagBatch = new HashMap<>();
                }
        }

    }

    private void batchTags(EntityInputBean entityInputBeans) {

        for (TagInputBean tag : entityInputBeans.getTags()) {
            String tagKey = getTagKey(tag);
            TagInputBean cachedTag = tagBatch.get(tagKey);
            if (cachedTag == null)
                tagBatch.put(tagKey, tag);
            else {
                cachedTag.mergeTags(tag);
            }
        }
    }

    private String getTagKey(TagInputBean tag) {
        return (tag.getKeyPrefix()!=null ?tag.getKeyPrefix()+"-":"")+tag.getCode() + "-" +tag.getLabel();
    }

    public void flush() throws FlockException {
        if (fdWriter.isSimulateOnly())
            return;
        try {

            entityLock.lock();
            batchTag(null, true, "");
            if ( entityBatch.size() >0)
                fdWriter.flushEntities(company, entityBatch, clientConfiguration);
            entityBatch.clear();

        } finally {
            entityLock.unlock();
        }

    }

    public List<EntityInputBean> getEntities() {
        return entityBatch;
    }

    public List<TagInputBean> getTags() {
        return new ArrayList<>(tagBatch.values());
    }
}
