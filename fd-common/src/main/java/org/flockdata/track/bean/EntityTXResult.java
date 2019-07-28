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

package org.flockdata.track.bean;

import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;


/**
 * @author mholdsworth
 * @since 16/06/2013
 */
public class EntityTXResult {

  private String auditKey;
  private String fortressName;
  private String fortressKey;
  private String documentType;
  private String code;
  private Long lastSystemChange;
  private Long fortressWhen = 0L;

  private EntityLog entityLog;

  private EntityTXResult() {
  }


  public EntityTXResult(Entity entity, EntityLog log) {
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
