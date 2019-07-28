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

package org.flockdata.store.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Encapsulate KV Content properties
 *
 * @author mholdsworth
 * @tag Payload, Store
 * @since 19/11/2014
 */
public class StorageBean implements StoredContent, Serializable {
  private EntityResultBean entity;
  private Object id;
  private String checksum;
  private ContentInputBean content = null;
  private String store;
  private String type;
  private boolean noResult;

  public StorageBean() {
    noResult = true;
  }

  public StorageBean(Object logId, Map<String, Object> oResult) {
    this(oResult);
    id = logId;

  }

  public StorageBean(Map<String, Object> json) {
    this.content = new ContentInputBean(json);
  }

  public StorageBean(Object key, ContentInputBean content) {
    this.content = content;
    this.id = key;
  }


  public StorageBean(TrackResultBean trackResultBean) {

    this.entity = new EntityResultBean(trackResultBean.getEntity());
    this.type = entity.getType();
    assert this.type != null;

    if (trackResultBean.getCurrentLog() != null) {
      if (trackResultBean.getCurrentLog().getLog() != null) {
        this.id = trackResultBean.getCurrentLog().getLog().getId();
        this.store = trackResultBean.getCurrentLog().getLog().getStorage();
      }
    }
    this.content = trackResultBean.getContentInput();
    if (this.content != null) {
      content.setCode(trackResultBean.getEntity().getCode());
      content.setKey(trackResultBean.getKey());
    }

  }

  public StorageBean(TrackResultBean trackResultBean, Store storeToTest) {
    this(trackResultBean);
    this.setStore(storeToTest.name());
  }

  public ContentInputBean getContent() {
    return content;
  }

  @JsonIgnore
  public String getAttachment() {
    if (content == null) {
      return null;
    }
    return content.getAttachment();
  }

  @JsonIgnore
  public Map<String, Object> getData() {
    if (content == null) {
      return null;
    }

    return content.getData();
  }

  /**
   * @return version of the contentModel used to create the payload
   */
  @JsonIgnore
  public Double getVersion() {
    return content.getVersion();
  }

  public String getChecksum() {

    assert getData() != null || getAttachment() != null;

    if (checksum != null) {
      return checksum;
    }

    Checksum crcChecksum = new CRC32();
    byte[] bytes;
    if (getAttachment() != null) {
      bytes = getAttachment().getBytes();
    } else {
      try {
        bytes = JsonUtils.toJsonBytes(getData());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    crcChecksum.update(bytes, 0, bytes.length);
    checksum = Long.toHexString(crcChecksum.getValue());
    return checksum;
  }

  @Override
  public Object getId() {
    return id;
  }

  public String getStore() {
    return store;
  }


  public void setStore(String store) {
    this.store = store;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public EntityResultBean getEntity() {
    return entity;
  }
}
