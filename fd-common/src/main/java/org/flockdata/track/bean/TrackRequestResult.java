/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.Collection;

/**
 * @author mholdsworth
 * @tag Contract, Track, Content
 * @since 20/07/2015
 */
public class TrackRequestResult implements Serializable {

  private String company;
  private String fortressCode;
  private String code;
  private String key;

  private boolean newEntity;
  private ContentInputBean.LogStatus logStatus;

  private Collection<String> serviceMessages = null;

  TrackRequestResult() {
  }

  public TrackRequestResult(TrackResultBean resultBean) {
    this();
    this.key = resultBean.getKey();
    this.code = resultBean.getEntity().getCode();
    this.fortressCode = resultBean.getEntity().getSegment().getFortress().getCode();
    logStatus = resultBean.getLogStatus();
    this.company = resultBean.getCompany().getCode();
    serviceMessages = resultBean.getServiceMessages();
    newEntity = resultBean.isNewEntity();
  }

  public String getKey() {
    return key;
  }

  public String getCode() {
    return code;
  }

  public String getFortressCode() {
    return fortressCode;
  }

  public String getCompany() {
    return company;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public ContentInputBean.LogStatus getLogStatus() {
    return logStatus;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Collection<String> getServiceMessages() {
    return serviceMessages;
  }

  public boolean isNewEntity() {
    return newEntity;
  }


}
