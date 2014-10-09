package com.auditbucket.transform;

import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:33 PM
 */
public class TrackBatcher {
    private List<EntityInputBean> entityBatch = new ArrayList<>();
    private Map<String, TagInputBean> tagBatch = new HashMap<>();
    private final String entitySync = "BatchSync";
    private final String tagSync = "TagSync";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TrackBatcher.class);

    private ProfileConfiguration importProfile;
    private int batchSize = 100;
    FdWriter fdWriter;

    public TrackBatcher(ProfileConfiguration importProfile, FdWriter writer, int batchSize){
        this.importProfile = importProfile;
        this.batchSize = batchSize;
        this.fdWriter = writer;

    }

    public void batchTag(TagInputBean tagInputBean, String message) throws FlockException {
        batchTag(tagInputBean, false, message);
    }

    public void batchEntity(EntityInputBean entityInputBean, boolean flush, String message) throws FlockException {

        synchronized (entitySync) {
            if (entityInputBean != null) {
                if (entityInputBean.getFortress() == null)
                    entityInputBean.setFortress(importProfile.getFortress());
                entityBatch.add(entityInputBean);
                batchTags(entityInputBean);
            }

            if (flush || entityBatch.size() == batchSize) {

                if (entityBatch.size() >= 1) {
                    logger.debug("Flushing....");
                    // process the tags independently to reduce the chance of a deadlock when processing the entity
                    fdWriter.flushTags(new ArrayList<>(tagBatch.values()));
                    fdWriter.flushEntities(entityBatch);
                    logger.debug("Flushed " + message + " Batch [{}]", entityBatch.size());
                }
                entityBatch = new ArrayList<>();
                tagBatch = new HashMap<>();
            }

        }

    }

    private void batchTag(TagInputBean tagInputBean, boolean flush, String message) throws FlockException {

        synchronized (tagSync) {
            if (tagInputBean != null)
                tagBatch.put(tagInputBean.getName() + tagInputBean.getLabel(), tagInputBean);

            if (flush || tagBatch.size() == batchSize) {
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


    public void flush(String canonicalName, ProfileConfiguration.DataType dataType) throws FlockException {
        if (fdWriter.isSimulateOnly())
            return;
        if (dataType.equals(ProfileConfiguration.DataType.TRACK)) {
            synchronized (entitySync) {
                fdWriter.flushEntities(entityBatch);
                entityBatch.clear();

            }
        } else {
            synchronized (tagSync) {
                batchTag(null, true, "");
            }
        }

    }

    public void flush(String message) throws FlockException{
        flush(message, ProfileConfiguration.DataType.TAG);
        flush(message, ProfileConfiguration.DataType.TRACK);

    }
}
