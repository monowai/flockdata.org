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
