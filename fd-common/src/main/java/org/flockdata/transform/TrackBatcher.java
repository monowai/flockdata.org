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
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
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
public class TrackBatcher {
    private List<EntityInputBean> entityBatch = new ArrayList<>();
    private Map<String, TagInputBean> tagBatch = new HashMap<>();
    private final Lock entityLock = new ReentrantLock();
    private final String tagSync = "TagSync";
    private Company company = null;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TrackBatcher.class);

    private ProfileConfiguration importProfile;
    private ClientConfiguration  clientConfiguration;
    FdWriter fdWriter;

    public TrackBatcher(ProfileConfiguration importProfile, FdWriter writer, ClientConfiguration configuration, Company company) {
        this.importProfile = importProfile;
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
                if (entityInputBean.getFortress() == null)
                    entityInputBean.setFortress(importProfile.getFortressName());
                if ( entityInputBean.getFortress() == null )
                    throw new FlockException("Unable to resolve the fortress name that owns this entity. Add this via your import profile.");

                entityBatch.add(entityInputBean);
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

    public int getEntityCount(){
        return entityBatch.size();
    }

    private void batchTag(TagInputBean tagInputBean, boolean flush, String message) throws FlockException {

        synchronized (tagSync) {
            if (tagInputBean != null)
                tagBatch.put(tagInputBean.getName() + tagInputBean.getLabel(), tagInputBean);

            if (flush || tagBatch.size() == clientConfiguration.getBatchSize()) {
                logger.debug("Flushing " + message + " Tag Batch [{}]", tagBatch.size());
                if (tagBatch.size() >= 0)
                    fdWriter.flushTags(new ArrayList<>(tagBatch.values()));
                logger.debug("Tag Batch Flushed");
                tagBatch = new HashMap<>();
            }
        }

    }

    private void batchTags(EntityInputBean entityInputBeans) {

        for (TagInputBean tag : entityInputBeans.getTags()) {
            String indexKey = tag.getCode() + tag.getLabel();
            TagInputBean cachedTag = tagBatch.get(indexKey);
            if (cachedTag == null)
                tagBatch.put(indexKey, tag);
            else {
                cachedTag.mergeTags(tag);
            }
        }
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
}
