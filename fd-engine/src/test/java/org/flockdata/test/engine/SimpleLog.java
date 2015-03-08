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

import org.flockdata.registration.model.FortressUser;
import org.flockdata.track.model.*;

/**
 * Created by mike on 6/03/15.
 */
public class SimpleLog implements Log {
    String checkSum;
    long id;

    public SimpleLog(long l) {
        this.id = l;
    }

    @Override
    public String getChecksum() {
        return checkSum;
    }

    @Override
    public void setChecksum(String checksum) {
        this.checkSum = checksum;
    }

    @Override
    public FortressUser getWho() {
        return null;
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public void setComment(String comment) {

    }

    @Override
    public void setTxRef(TxRef txRef) {

    }

    @Override
    public ChangeEvent getEvent() {
        return event;
    }
    boolean compressed = false;
    Log previousLog;
    @Override
    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public void setPreviousLog(Log previousChange) {
        this.previousLog = previousChange;
    }

    @Override
    public Log getPreviousLog() {
        return previousLog;
    }

    @Override
    public Long getId() {
        return id;
    }
    private String whatStore;
    @Override
    public String getWhatStore() {
        return whatStore;
    }

    @Override
    public void setWhatStore(String storage) {
        this.whatStore = storage;
    }
    ChangeEvent event;
    @Override
    public void setEvent(ChangeEvent event) {
        this.event = event;
    }

    @Override
    public void setCompressed(Boolean compressed) {

    }
    EntityLog entityLog;
    @Override
    public EntityLog getEntityLog() {
        return entityLog;
    }

    private KvContent kvContent;
    @Override
    public void setContent(KvContent kvContent) {
        this.kvContent = kvContent;
    }

    @Override
    public Double getProfileVersion() {
        return 0d;
    }

    @Override
    public KvContent getContent() {
        return kvContent;
    }

    @Override
    public void setEntityLog(EntityLog newLog) {
        this.entityLog = newLog;
    }

    String contentType;
    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    String fileName;
    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
