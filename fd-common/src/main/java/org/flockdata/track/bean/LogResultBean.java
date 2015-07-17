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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.EntityLog;
import org.flockdata.model.TxRef;

import java.io.Serializable;

/**
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
public class LogResultBean implements Serializable {
    public static final String OK = "OK";
    private String fortress;
    private String documentType;
    private String callerRef;
    private ContentInputBean.LogStatus status = ContentInputBean.LogStatus.OK;

    private String fortressUser;
    private String txReference = null;
    private Long sysWhen;
    private transient EntityLog logToIndex;
    private boolean logIgnored = false;

    private LogResultBean() {
    }

    public LogResultBean(ContentInputBean content) {
        this();
        setFortressUser(content.getFortressUser());

    }

    public ContentInputBean.LogStatus getStatus() {
        return status;
    }

    public void setTxReference(TxRef txReference) {
        this.txReference = txReference.getName();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTxReference() {
        return txReference;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFortressUser() {
        return fortressUser;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCallerRef() {
        return callerRef;
    }

    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public void setStatus(ContentInputBean.LogStatus status) {
        this.status = status;
    }

    public void setSysWhen(Long sysWhe) {
        this.sysWhen = sysWhe;
    }

    public Long getSysWhen() {
        return sysWhen;
    }

    public void setLogToIndex(EntityLog logToIndex) {
        this.logToIndex = logToIndex;
    }

    @JsonIgnore
    public EntityLog getLogToIndex() {
        return logToIndex;
    }

    public void setLogIgnored() {
        this.logIgnored = true;
    }

    public boolean isLogIgnored() {
        return logIgnored ||
                getStatus() == ContentInputBean.LogStatus.IGNORE ||
                getStatus() == ContentInputBean.LogStatus.TRACK_ONLY;
    }
}
