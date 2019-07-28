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

package org.flockdata.engine.integration.neorest;

import com.google.common.net.MediaType;
import javax.annotation.PostConstruct;
import org.flockdata.helper.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

/**
 * @author mholdsworth
 * @since 15/07/2015
 */
@Profile("neorest")
public class NeoRequestBase {

  @Autowired
  FdNeoChannels channels;
  private ObjectToJsonTransformer transformer;

  public String getEntityUrl() {
    return channels.getUriRoot() + "/entity";
  }

  public String getEntityTagUrl() {
    return channels.getUriRoot() + "/entitytag";
  }

  public String getLogUrl() {
    return channels.getUriRoot() + "/log";
  }

  public String getLastLog() {
    return getForEntity() + "/last";
  }

  public String getEntityLog() {
    return getForEntity() + "/{logId}";
  }

  public String getForEntity() {
    return getLogUrl() + "/{entityId}";
  }

  public String getFindLogsBeforeUrl() {
    return getForEntity() + "/before/{time}";
  }

  public String getTagUrl() {
    return channels.getUriRoot() + "/tag";
  }

  public String getFindTagUrl() {
    return getTagUrl() + "/{label}/{code}";
  }

  public String getAliasUrl() {
    return getTagUrl() + "/alias";
  }

  public String getKeyUrl() {
    return getEntityUrl() + "/{key}";
  }

  public String getLabelFindUrl() {
    return getEntityUrl() + "/admin/{fortressId}/{label}/{skipCount}";
  }

  public String getCallerRefUrl() {
    return getEntityUrl() + "/{fortressId}/{docId}/{callerRef}";
  }

  public String getEntityTag() {
    return getEntityTagUrl() + "/{entityId}/{tagType}/{tagCode}/{relationshipType}";
  }

  public String getEntityTags() {
    return getEntityTagUrl() + "/{entityId}";
  }

  @PostConstruct
  public void createTransformer() {
    transformer = new ObjectToJsonTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );
    transformer.setContentType(MediaType.JSON_UTF_8.toString());
    //return transformer;
  }

  public ObjectToJsonTransformer getTransformer() {
    return transformer;
  }

}
