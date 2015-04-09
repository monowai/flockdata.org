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

package org.flockdata.test.engine;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.Log;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

/**
 * Simple POJO for functional tests
 * Created by mike on 6/03/15.
 */
public class SimpleEntity implements Entity, Serializable {
    private long fortressCreate;
    private String indexName;
    private String name;
    private String description;
    private String event ;
    private Long dateCreated;
    private Long lastUpdate;
    private String callerKeyRef;
    private String callerRef;
    private Fortress fortress;
    private  String metaKey;
    private boolean isNew;
    String docType;

    public SimpleEntity (){};
    public SimpleEntity(String uniqueKey,Fortress fortress,  @NotEmpty EntityInputBean entityInput, @NotEmpty String documentType) throws FlockException {
        this();

        assert documentType != null;
        assert fortress!=null;

        metaKey = uniqueKey;
        this.fortress = fortress;//(FortressNode)documentType.getFortress();

        isNew = true;

        docType = documentType.toLowerCase();
        callerRef = entityInput.getCallerRef();
        callerKeyRef = this.fortress.getId() + "." + documentType.hashCode() + "." + (callerRef != null ? callerRef : metaKey);

        if (entityInput.getName() == null || entityInput.getName().equals(""))
            this.name = (callerRef == null ? docType : (docType + "." + callerRef));
        else
            this.name = entityInput.getName();

        this.description = entityInput.getDescription();

        indexName = EntitySearchSchema.parseIndex(this.fortress);

        Date when = entityInput.getWhen();

        if (when == null)
            fortressCreate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.fortress.getTimeZone()))).getMillis();
        else
            fortressCreate = new DateTime (when.getTime()).getMillis();//new DateTime( when.getTime(), DateTimeZone.forTimeZone(TimeZone.getTimeZone(entityInput.getMetaTZ()))).toDate().getTime();

        lastUpdate = 0l;

        if ( entityInput.isMetaOnly())
            this.event = entityInput.getEvent();
        this.suppressSearch(entityInput.isSearchSuppressed());

    }
    @Override
    public Long getId() {
        return null;
    }

    @Override
    public Fortress getFortress() {
        return fortress;
    }

    @Override
    public String getDocumentType() {
        return docType;
    }

    @Override
    public String getMetaKey() {
        return metaKey;
    }

    long fortressLastWhen =0l;
    @Override
    public void setFortressLastWhen(Long fortressWhen) {
        this.fortressLastWhen = fortressWhen;
    }

    @Override
    public Long getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public Long getFortressDateUpdated() {
        return fortressLastWhen;
    }

    private FortressUser lastUser;
    @Override
    public void setLastUser(FortressUser user) {
        this.lastUser = user;
    }

    @Override
    public FortressUser getLastUser() {
        return lastUser;
    }

    private FortressUser fortressUser;
    @Override
    public void setCreatedBy(FortressUser fortressUser) {
        this.fortressUser = fortressUser;
    }
    @Override
    public FortressUser getCreatedBy() {
        return fortressUser;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void bumpUpdate() {

    }

    @Override
    public boolean isSearchSuppressed() {
        return false;
    }

    @Override
    public void suppressSearch(boolean searchSuppressed) {

    }
    private String searchKey = null;
    @Override
    public void setSearchKey(String parentKey) {
        this.searchKey = parentKey;

    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public String getCallerRef() {
        return callerRef;
    }

    @Override
    public long getWhenCreated() {
        return fortressCreate;
    }

    @Override
    public DateTime getFortressDateCreated() {
        return new DateTime(fortressCreate);
    }

    @Override
    public String getEvent() {
        return event;
    }
    Log lastChange;
    @Override
    public void setLastChange(Log newChange) {
        this.lastChange = newChange;
    }

    @Override
    public Log getLastChange() {
        return lastChange;
    }

    @Override
    public String getCallerKeyRef() {
        return callerKeyRef;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void bumpSearch() {

    }

    @Override
    public void addLabel(String label) {

    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public void setNew() {
        setNew(true);
    }

    @Override
    public void setNew(boolean status) {
        isNew = status;
    }

}
