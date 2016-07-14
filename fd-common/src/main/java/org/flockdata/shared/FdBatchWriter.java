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
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.PayloadBatcher;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:33 PM
 */
@Component
@Configuration
@Profile({"!dev"})
public class FdBatchWriter implements PayloadBatcher {
    private List<EntityInputBean> entityBatch = new ArrayList<>();
    private Map<String, TagInputBean> tagBatch = new HashMap<>();
    private final Lock entityLock = new ReentrantLock();
    private final Lock tagLock = new ReentrantLock();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FdBatchWriter.class);

    @Autowired
    private ClientConfiguration  clientConfiguration;

    @Autowired (required = false) // Misc impls provided in fd-client, fd-engine etc.
    FdWriter fdWriter;

    public FdBatchWriter(){}

    /**
     * POJO configuration approach
     * @param writer    writer to send payloads to
     * @param configuration configuration properties
     */
    public FdBatchWriter(FdWriter writer, ClientConfiguration configuration) {
        this();
        this.clientConfiguration = configuration;
        this.fdWriter = writer;
        logger.info("Configuration {}", clientConfiguration);

    }

    @Override
    public void writeTag(TagInputBean tagInputBean, String message) throws FlockException {
        writeTag(tagInputBean, false, message);
    }

    @Override
    public void writeEntity(EntityInputBean entityInputBean) throws FlockException {
        writeEntity(entityInputBean, false);
    }

    @Override
    public void writeEntity(EntityInputBean entityInputBean, boolean doWrite) throws FlockException {
        if ( fdWriter == null )
            throw new FlockException( "No valid FdWriter could be found. Please provide an implementation");
        try {
            entityLock.lock();
            if (entityInputBean != null) {

                if ( entityInputBean.getFortress() == null )
                    throw new FlockException("Unable to resolve the fortress name that owns this entity. Add this via a content model with the fortressName attribute.");

                if ( !validDocumentType(entityInputBean)  )
                    throw new FlockException("Unable to resolve the document type name that defines this entity. Add this via a content model with the documentName attribute.");

                int existingIndex = getExistingIndex(entityInputBean);

                if ( existingIndex>-1){  // Additive behaviour - merge tags in this entity into one we already know about
                    EntityInputBean masterEntity = entityBatch.get(existingIndex);
                    masterEntity.merge(entityInputBean);
                } else {
                    entityBatch.add(entityInputBean);
                }
                writeTags(entityInputBean);
            }

            if ( clientConfiguration.getBatchSize()> 0 && (doWrite || entityBatch.size() >=  clientConfiguration.getBatchSize())) {

                if (entityBatch.size() >0 ) {
                    logger.debug("Writing....");
                    fdWriter.writeEntities(entityBatch);
                    logger.debug("Wrote Batch [{}]", entityBatch.size());
                }
                entityBatch = new ArrayList<>();
                tagBatch = new HashMap<>();
            }

        } finally {
            entityLock.unlock();
        }

    }

    private boolean validDocumentType(EntityInputBean entityInputBean) {
        return !(entityInputBean.getDocumentType() == null || entityInputBean.getDocumentType().getName().equals(""));

    }

    /**
     * determines if an entity already being tracked can be considered to be merged with
     * @param entityInputBean incoming entity
     * @return index of an existing EIB or -1 if it should be ignored
     */
    private int getExistingIndex(EntityInputBean entityInputBean) {
        int existingIndex =-1;
        if ( (entityInputBean.getCode()!= null || entityInputBean.getKey()!=null ) && entityInputBean.getContent() == null )
            existingIndex = entityBatch.indexOf(entityInputBean);
        return existingIndex;
    }

    private void writeTag(TagInputBean tagInputBean, boolean flush, String message) throws FlockException {

        try {
            tagLock.lock();
            if (tagInputBean != null) {
                if ( tagInputBean.getCode()==null || tagInputBean.getCode().equals("")) {
                    logger.error("Attempting to create a tag without a code value. Code is a required field []" + tagInputBean);
                    return ;
                }

                tagBatch.put(getTagKey(tagInputBean), tagInputBean);

            }

            if ( tagBatch.size() >0 )
                if (flush || tagBatch.size() >= clientConfiguration.getBatchSize()) {
                    logger.debug("Writing " + message + " Tag Batch [{}]", tagBatch.size());
                    if (tagBatch.size() > 0)
                        fdWriter.writeTags(new ArrayList<>(tagBatch.values()));
                    logger.debug("Wrote Tag Batch");
                    tagBatch = new HashMap<>();
                }
        } finally {
            tagLock.unlock();
        }

    }

    public String getApiKey() {
        return clientConfiguration.getApiKey();
    }

    @SuppressWarnings("unused")
    public Collection<String> getFilesToImport(){
        return clientConfiguration.getFilesToImport();
    }

    private void writeTags(EntityInputBean entityInputBeans) {

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

    @Override
    public void flush()  {
        try {
            entityLock.lock();
            try {
                writeTag(null, true, "");
                if ( entityBatch.size() >0)
                    fdWriter.writeEntities(entityBatch);
                entityBatch.clear();
            } catch (FlockException e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
            reset();

        } finally {
            entityLock.unlock();
        }

    }

    @Override
    public void reset(){
        entityBatch.clear();
        tagBatch.clear();
    }

    @Override
    public List<EntityInputBean> getEntities() {
        return entityBatch;
    }

    @Override
    public List<TagInputBean> getTags() {
        return new ArrayList<>(tagBatch.values());
    }

    @Override
    public ContentModel getContentModel(String model) {
        try {
            return fdWriter.getContentModel(clientConfiguration, model);
        } catch (IOException e) {
            logger.error("Failed to get the ContentModel ["+model+"]. ", e);
        }
        return null;
    }
}
