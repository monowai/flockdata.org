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

package org.flockdata.track;

import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressUser;
import org.flockdata.track.bean.EntityInputBean;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 19/06/15.
 */
public class EntityPayload {

    String tenant = "";
    Collection<EntityInputBean> entities = new ArrayList<>();
    Fortress fortress;
    DocumentType documentType = null;
    private FortressUser createdBy;
    private Entity entity = null;

    EntityPayload() {
    }

    public EntityPayload(Fortress fortress, DocumentType documentType, EntityInputBean entityInputBean) {
        this();
        this.fortress = fortress;
        entities.add(entityInputBean);
        this.documentType = documentType;

    }

    /**
     * Used when updating an entity
     *
     * @param entity
     */
    public EntityPayload(Entity entity) {
        this();
        this.entity = entity;
    }

    // An entity that is to be saved
    public Entity getEntity() {
        return entity;
    }

    public EntityPayload setTenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    public EntityPayload setEntities(Collection<EntityInputBean> entities) {
        this.entities = entities;
        return this;
    }

    public String getTenant() {
        return tenant;
    }

    public Collection<EntityInputBean> getEntities() {
        return entities;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public void setFortress(Fortress fortress) {
        this.fortress = fortress;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public void setCreatedBy(FortressUser createdBy) {
        this.createdBy = createdBy;
    }

    public FortressUser getCreatedBy() {
        return createdBy;
    }
}
