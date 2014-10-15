package com.auditbucket.transform;

import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
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
    private Company company = null;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TrackBatcher.class);

    private ProfileConfiguration importProfile;
    private int batchSize = 100;
    FdWriter fdWriter;
    boolean async = false;

    public TrackBatcher(ProfileConfiguration importProfile, FdWriter writer, int batchSize, Company company, Boolean async) {
        this.importProfile = importProfile;
        this.batchSize = batchSize;
        this.fdWriter = writer;
        this.company = company;
        this.async = async;

    }

    public TrackBatcher(ProfileConfiguration profileConfiguration, FdWriter writer, int batchSize, Company company) {
        this (profileConfiguration, writer, batchSize, company, false);
        this.company = company;
    }

    public void batchTag(TagInputBean tagInputBean, String message) throws FlockException {
        batchTag(tagInputBean, false, message);
    }

    public void batchEntity(EntityInputBean entityInputBean) throws FlockException {
        batchEntity(entityInputBean, false);
    }

    public void batchEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException {

        synchronized (entitySync) {
            if (entityInputBean != null) {
                if (entityInputBean.getFortress() == null)
                    entityInputBean.setFortress(importProfile.getFortressName());
                entityBatch.add(entityInputBean);
                batchTags(entityInputBean);
            }

            if (flush || entityBatch.size() == batchSize) {

                if (entityBatch.size() >= 1) {
                    logger.debug("Flushing....");
                    // process the tags independently to reduce the chance of a deadlock when processing the entity
                    fdWriter.flushTags(new ArrayList<>(tagBatch.values()));
                    fdWriter.flushEntities(company, entityBatch, async );
                    logger.debug("Flushed Batch [{}]", entityBatch.size());
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


    public void flush() throws FlockException {
        if (fdWriter.isSimulateOnly())
            return;
        synchronized (entitySync) {
            fdWriter.flushEntities(company, entityBatch, async);
            entityBatch.clear();

        }
        synchronized (tagSync) {
            batchTag(null, true, "");
        }

    }
}
