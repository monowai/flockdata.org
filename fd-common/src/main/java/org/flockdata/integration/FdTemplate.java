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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag FdClient, Batch, Track
 * @since 7/10/2014
 */
@Service
@Configuration
public class FdTemplate implements Template {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FdTemplate.class);
  private final Lock entityLock = new ReentrantLock();
  private final Lock tagLock = new ReentrantLock();
  private List<EntityInputBean> entityBatch = new ArrayList<>();
  private Map<String, TagInputBean> tagBatch = new HashMap<>();
  private ClientConfiguration clientConfiguration;

  private FdIoInterface fdIoInterface;

  protected FdTemplate() {
  }

  @Autowired
  public FdTemplate(ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  @Autowired
  public void setClientConfiguration(ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  /**
   * @return Implementation of the IO interface being used to communicate with the service
   */
  @Override
  public FdIoInterface getFdIoInterface() {
    return fdIoInterface;
  }

  @Autowired(required = false)
  public void setFdIoInterface(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }

  @Override
  public void writeTag(TagInputBean tagInputBean) throws FlockException {
    writeTags(Collections.singletonList(tagInputBean), false);
  }

  @Override
  public void writeTags(Collection<TagInputBean> tagInputBeans) throws FlockException {
    writeTags(tagInputBeans, false);
  }

  @Override
  public void writeEntity(EntityInputBean entityInputBean) throws FlockException {
    writeEntity(entityInputBean, false);
  }

  @Override
  public void writeEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException {
    if (fdIoInterface == null) {
      throw new FlockException("No valid FdIoHandler could be found. Please provide an implementation");
    }
    try {
      entityLock.lock();

      if (entityInputBean != null) {

        if (entityInputBean.getFortress() == null) {
          throw new FlockException("Unable to resolve the fortress name that owns this entity. Add this via a content model with the fortressName attribute.");
        }

        if (!validDocumentType(entityInputBean)) {
          throw new FlockException("Unable to resolve the document type name that defines this entity. Add this via a content model with the documentName attribute.");
        }

        int existingIndex = getExistingIndex(entityInputBean);

        if (existingIndex > -1) {  // Additive behaviour - merge tags in this entity into one we already know about
          EntityInputBean masterEntity = entityBatch.get(existingIndex);
          masterEntity.merge(entityInputBean);
        } else {
          entityBatch.add(entityInputBean);
        }
        writeTags(entityInputBean);
      }

      if (clientConfiguration.batchSize() > 0 &&
          (flush || entityBatch.size() >= clientConfiguration.batchSize())) {

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
    if (entityInputBean.getCode() != null || entityInputBean.getKey() != null) {
      existingIndex = entityBatch.indexOf(entityInputBean);
    }
    return existingIndex;
  }

  private void writeTags(Collection<TagInputBean> tagInputBeans, boolean forceFlush) throws FlockException {
    if (tagInputBeans == null) {
      return;
    }
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

        if (tagBatch.size() > 0) {
          if (forceFlush || tagBatch.size() >= clientConfiguration.batchSize()) {
            logger.debug("Writing Tag Batch [{}]", tagBatch.size());
            if (tagBatch.size() > 0) {
              fdIoInterface.writeTags(new ArrayList<>(tagBatch.values()));
            }
            logger.debug("Wrote Tag Batch");
            tagBatch = new HashMap<>();
          }
        }
      }

    } finally {
      tagLock.unlock();
    }

  }

  public String getApiKey() {
    return clientConfiguration.apiKey();
  }

  private void writeTags(EntityInputBean entityInputBeans) {

    for (TagInputBean tag : entityInputBeans.getTags()) {
      String tagKey = getTagKey(tag);
      TagInputBean cachedTag = tagBatch.get(tagKey);
      if (cachedTag == null) {
        tagBatch.put(tagKey, tag);
      } else {
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
        writeTags(null, true);
        if (entityBatch.size() > 0) {
          fdIoInterface.writeEntities(entityBatch);
        }
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

  @Override
  public SystemUserResultBean validateConnectivity() throws FlockException {
    return fdIoInterface.validateConnectivity();
  }

}
