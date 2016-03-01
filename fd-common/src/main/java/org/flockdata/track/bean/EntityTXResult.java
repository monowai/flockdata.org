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

package org.flockdata.track.bean;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityLog;
import org.flockdata.model.Log;


/**
 * User: Mike Holdsworth
 * Date: 16/06/13
 * Time: 6:12 PM
 */
public class EntityTXResult {

    private String auditKey;
    private String fortressName;
    private String fortressKey;
    private String documentType;
    private String code;
    private Long lastSystemChange;
    private Long fortressWhen = 0l;

    private EntityLog entityLog;

    private EntityTXResult() {
    }


    public EntityTXResult(Entity entity, Log change, EntityLog log) {
        this();
        this.fortressWhen = log.getFortressWhen();
        this.auditKey = entity.getKey();
        this.documentType = entity.getType();
        this.code = entity.getCode();
        this.fortressName = entity.getSegment().getFortress().getName();
        this.lastSystemChange = entity.getLastUpdate();
        this.entityLog = log;
    }

    public Object getEntityLog() {
        return entityLog;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getFortressKey() {
        return fortressKey;
    }

    public String getDocumentType() {
        return documentType;
    }

    public long getLastSystemChange() {
        return lastSystemChange;
    }

    public String getCode() {
        return code;
    }
}
