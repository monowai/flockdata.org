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

package org.flockdata.shared;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.PayloadBatcher;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
@Profile({"fd-batch"})
public class FdBatchWriter implements PayloadBatcher {
    private List<EntityInputBean> entityBatch = new ArrayList<>();
    private Map<String, TagInputBean> tagBatch = new HashMap<>();
    private final Lock entityLock = new ReentrantLock();
    private final Lock tagLock = new ReentrantLock();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FdBatchWriter.class);

    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired(required = false)
    FdIoInterface fdIoInterface;   // Misc impls provided in fd-client, fd-engine etc.

    protected FdBatchWriter () {}

    /**
     * POJO configuration approach
     *
     * @param writer        writer to send payloads to
     * @param configuration configuration properties
     */
    public FdBatchWriter(FdIoInterface writer, ClientConfiguration configuration) {
        this();
        this.clientConfiguration = configuration;
        this.fdIoInterface = writer;
        logger.info("Configuration {}", clientConfiguration);

    }

    @Override
    public void writeTag(TagInputBean tagInputBean, String message) throws FlockException {
        writeTags(Collections.singletonList(tagInputBean), false, message);
    }

    @Override
    public void writeTags(Collection<TagInputBean> tagInputBeans, String message) throws FlockException {
        writeTags(tagInputBeans, false, message);
    }

    @Override
    public void writeEntity(EntityInputBean entityInputBean) throws FlockException {
        writeEntity(entityInputBean, false);
    }

    @Override
    public void writeEntity(EntityInputBean entityInputBean, boolean doWrite) throws FlockException {
        if (fdIoInterface == null)
            throw new FlockException("No valid FdIoHandler could be found. Please provide an implementation");
        try {
            entityLock.lock();
            if (entityInputBean != null) {

                if (entityInputBean.getFortress() == null)
                    throw new FlockException("Unable to resolve the fortress name that owns this entity. Add this via a content model with the fortressName attribute.");

                if (!validDocumentType(entityInputBean))
                    throw new FlockException("Unable to resolve the document type name that defines this entity. Add this via a content model with the documentName attribute.");

                int existingIndex = getExistingIndex(entityInputBean);

                if (existingIndex > -1) {  // Additive behaviour - merge tags in this entity into one we already know about
                    EntityInputBean masterEntity = entityBatch.get(existingIndex);
                    masterEntity.merge(entityInputBean);
                } else {
                    entityBatch.add(entityInputBean);
                }
                writeTags(entityInputBean);
            }

            if (clientConfiguration.getBatchSize() > 0 && (doWrite || entityBatch.size() >= clientConfiguration.getBatchSize())) {

                if (entityBatch.size() > 0) {
                    logger.debug("Writing....");
                    fdIoInterface.writeEntities(entityBatch);
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
     *
     * @param entityInputBean incoming entity
     * @return index of an existing EIB or -1 if it should be ignored
     */
    private int getExistingIndex(EntityInputBean entityInputBean) {
        int existingIndex = -1;
        if ((entityInputBean.getCode() != null || entityInputBean.getKey() != null) && entityInputBean.getContent() == null)
            existingIndex = entityBatch.indexOf(entityInputBean);
        return existingIndex;
    }

    private void writeTags(Collection<TagInputBean> tagInputBeans, boolean forceFlush, String message) throws FlockException {
        if ( tagInputBeans== null )
            return;
        try {
            tagLock.lock();
            for (TagInputBean tagInputBean : tagInputBeans) {

                if (tagInputBean != null) {
                    if (tagInputBean.getCode() == null || tagInputBean.getCode().equals("")) {
                        logger.error("Attempting to create a tag without a code value. Code is a required field []" + tagInputBean);
                        return;
                    }

                    tagBatch.put(getTagKey(tagInputBean), tagInputBean);
                }

                if (tagBatch.size() > 0)
                    if (forceFlush || tagBatch.size() >= clientConfiguration.getBatchSize()) {
                        logger.debug("Writing " + message + " Tag Batch [{}]", tagBatch.size());
                        if (tagBatch.size() > 0)
                            fdIoInterface.writeTags(new ArrayList<>(tagBatch.values()));
                        logger.debug("Wrote Tag Batch");
                        tagBatch = new HashMap<>();
                    }
            }

        } finally {
            tagLock.unlock();
        }

    }

    public String getApiKey() {
        return clientConfiguration.getApiKey();
    }

    @SuppressWarnings("unused")
    public Collection<String> getFilesToImport() {
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
        return (tag.getKeyPrefix() != null ? tag.getKeyPrefix() + "-" : "") + tag.getCode() + "-" + tag.getLabel();
    }

    @Override
    public void flush() {
        try {
            entityLock.lock();
            try {
                writeTags(null, true, "");
                if (entityBatch.size() > 0)
                    fdIoInterface.writeEntities(entityBatch);
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
    public void reset() {
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

}
