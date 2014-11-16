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

package org.flockdata.track.bean;

import org.flockdata.track.model.Entity;

/**
 * User: mike
 * Date: 17/11/14
 * Time: 8:47 AM
 */
public class EntityBean {
    private String metaKey;
    private String fortressCode;
    private String callerRef;
    private String documentType;
    private String createdBy;
    private long whenCreated;

    public EntityBean (){

    }
    public EntityBean(Entity entity){
        this();
        if ( entity != null ) {
            this.metaKey = entity.getMetaKey();
            fortressCode = entity.getFortress().getCode();
            documentType = entity.getDocumentType();
            callerRef = entity.getCallerRef();

            createdBy = entity.getCreatedBy().getCode();
            whenCreated = entity.getWhenCreated();
        }
    }

    public String getDocumentType() {
        return documentType;
    }

    void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getWhenCreated() {
        return whenCreated;
    }

    void setWhenCreated(long whenCreated) {
        this.whenCreated = whenCreated;
    }
    public String getMetaKey() {
        return metaKey;
    }

    void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getFortressCode() {
        return fortressCode;
    }

    void setFortressCode(String fortressCode) {
        this.fortressCode = fortressCode;
    }

    public String getCallerRef() {
        return callerRef;
    }

    void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }
}
