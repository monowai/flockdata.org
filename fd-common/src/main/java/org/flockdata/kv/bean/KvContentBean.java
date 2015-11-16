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

package org.flockdata.kv.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Entity;
import org.flockdata.kv.KvContent;
import org.flockdata.model.Log;
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
public class KvContentBean implements KvContent, Serializable{
    private Long id;
    private String checksum;
    private ContentInputBean content = null;
    private String bucket = null;
    private String storage;

    KvContentBean() {
    }

    public KvContentBean(Log log, ContentInputBean contentInput) {
        this.content = contentInput;
        if ( log!=null )
            id = log.getId();
    }
    public KvContentBean(Log log, Map<String, Object> oResult) {
        this(oResult);
        if ( log!=null )
            id = log.getId();

    }

    public KvContentBean(Map<String, Object> json) {
        this.content = new ContentInputBean(json);
    }

    public KvContentBean(Long key, ContentInputBean content){
        this();
        this.content = content;
        this.id = key;
    }


    public KvContentBean(TrackResultBean trackResultBean) {
        this();
        // ToDo: Code Smell - seems like we should have already got this from the KvManager already prepared
        this.bucket = parseBucket(trackResultBean.getEntity());
        if (trackResultBean.getCurrentLog() != null) {
            if ( trackResultBean.getCurrentLog().getLog()!=null) {
                this.id = trackResultBean.getCurrentLog().getLog().getId();
                this.storage= trackResultBean.getCurrentLog().getLog().getStorage();
            }
            this.content = trackResultBean.getContentInput();
            content.setCallerRef(trackResultBean.getEntity().getCode());
            content.setMetaKey(trackResultBean.getMetaKey());
        }
    }

//    public static String parseBucket(EntityBean entity) {
//        if ( entity == null )
//            return null;
//        return (entity.getIndexName() + "/" + entity.getType()).toLowerCase();
//    }

    public static String parseBucket(Entity entity) {
        // ToDo: Figure this out - DAT-419
        if ( entity == null )
            return null;
        return (entity.getSegment().getKey()).toLowerCase();
    }

    public ContentInputBean getContent() {
        return content;
    }

    @JsonIgnore
    public String getAttachment() {
        if ( content == null )
            return null;
        return content.getAttachment();
    }

    @JsonIgnore
    public Map<String, Object> getWhat() {
        if ( content == null )
            return null;

        return content.getWhat();
    }

    @JsonIgnore
    /**
     *
     * returns the version of the contentProfile used to create the payload
     */
    public Double getVersion(){
        return content.getpVer();
    }

    public String getChecksum() throws IOException {
        if ( checksum!=null )
            return checksum;
        Checksum crcChecksum = new CRC32();
        byte[] bytes;
        if ( getAttachment() != null )
            bytes =getAttachment().getBytes();
        else
            bytes= JsonUtils.getObjectAsJsonBytes(getWhat());
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

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }
}
