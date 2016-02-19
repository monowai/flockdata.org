/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.store.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.store.StoreContent;
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
public class StoreBean implements StoreContent, Serializable {
    private Long id;
    private String checksum;
    private ContentInputBean content = null;
    private String bucket = null;
    private String store;

    private DocumentType documentType = null;

    StoreBean() {
    }

    public StoreBean(Long logId, Map<String, Object> oResult) {
        this(oResult);
        id = logId;

    }

    public StoreBean(Map<String, Object> json) {
        this.content = new ContentInputBean(json);
    }

    public StoreBean(Long key, ContentInputBean content) {
        this();
        this.content = content;
        this.id = key;
    }


    public StoreBean(TrackResultBean trackResultBean) {
        this();
        this.documentType = trackResultBean.getDocumentType();
        this.bucket = parseBucket(trackResultBean.getEntity());


        if (trackResultBean.getCurrentLog() != null) {
            if (trackResultBean.getCurrentLog().getLog() != null) {
                this.id = trackResultBean.getCurrentLog().getLog().getId();
                this.store = trackResultBean.getCurrentLog().getLog().getStorage();
            }
            this.content = trackResultBean.getContentInput();
            if (this.content != null) {
                content.setCode(trackResultBean.getEntity().getCode());
                content.setMetaKey(trackResultBean.getMetaKey());
            }
        }
    }

    public static String parseBucket(Entity entity) {
        // ToDo: Figure this out - DAT-419
        if (entity == null)
            return null;
        return (entity.getSegment().getKey()).toLowerCase();
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
        if (checksum != null)
            return checksum;
        Checksum crcChecksum = new CRC32();
        byte[] bytes;
        if (getAttachment() != null)
            bytes = getAttachment().getBytes();
        else
            bytes = JsonUtils.toJsonBytes(getData());
        crcChecksum.update(bytes, 0, bytes.length);
        checksum = Long.toHexString(crcChecksum.getValue()).toUpperCase();
        return checksum;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getBucket() {
        return bucket;
    }

    @Override
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }
}
