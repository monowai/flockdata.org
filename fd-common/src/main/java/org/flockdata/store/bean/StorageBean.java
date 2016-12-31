/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.store.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.TrackResultBean;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Encapsulate KV Content properties
 * @author mholdsworth
 * @since 19/11/2014
 * @tag Payload, Store
 */
public class StorageBean implements StoredContent, Serializable {
    private EntityResultBean entity;
    private Object id;
    private String checksum;
    private ContentInputBean content = null;
    private String store;
    private String type ;
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
        assert this.type!=null;

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

    public String getChecksum() {

        assert getData()!=null || getAttachment()!=null;

        if (checksum != null)
            return checksum;

        Checksum crcChecksum = new CRC32();
        byte[] bytes;
        if (getAttachment() != null)
            bytes = getAttachment().getBytes();
        else
            try {
                bytes = JsonUtils.toJsonBytes(getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
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
    public EntityResultBean getEntity(){
        return entity;
    }
}
