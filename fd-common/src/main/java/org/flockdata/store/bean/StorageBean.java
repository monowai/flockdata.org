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

package org.flockdata.store.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Entity;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Encapsulate KV Content properties
 * User: mike
 * Date: 19/11/14
 * Time: 2:41 PM
 */
public class StorageBean implements StoredContent, Serializable {
    private Entity entity;
    private Object id;
    private String checksum;
    private ContentInputBean content = null;
    private String store;
    private String type ;

    StorageBean() {
    }

    public StorageBean(Object logId, Map<String, Object> oResult) {
        this(oResult);
        id = logId;

    }

    public StorageBean(Map<String, Object> json) {
        this.content = new ContentInputBean(json);
    }

    public StorageBean(Object key, ContentInputBean content) {
        this();
        this.content = content;
        this.id = key;
    }


    public StorageBean(TrackResultBean trackResultBean) {
        this();
        this.entity = trackResultBean.getEntity();
        this.type = entity.getType();
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

    public ContentInputBean getContent() {
        return content;
    }

    @JsonIgnore
    public String getAttachment() {
        if (content == null)
            return null;
        return content.getAttachment();
    }

    @JsonIgnore
    public Map<String, Object> getData() {
        if (content == null)
            return null;

        return content.getData();
    }

    @JsonIgnore
    /**
     *
     * returns the version of the contentProfile used to create the payload
     */
    public Double getVersion() {
        return content.getpVer();
    }

    public String getChecksum() throws IOException {

        assert getData()!=null || getAttachment()!=null;

        if (checksum != null)
            return checksum;

        Checksum crcChecksum = new CRC32();
        byte[] bytes;
        if (getAttachment() != null)
            bytes = getAttachment().getBytes();
        else
            bytes = JsonUtils.toJsonBytes(getData());
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
    public Entity getEntity(){
        return entity;
    }
}
