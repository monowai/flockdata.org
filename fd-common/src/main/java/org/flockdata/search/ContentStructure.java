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

package org.flockdata.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Meta structure of a search index
 * Groups fields in to FlockData tags, data elements and ElasticSearch internal types
 *
 * @author mholdsworth
 * @tag Search
 * @since 31/08/2016
 */
public class ContentStructure {
  @JsonProperty
  private String index;
  @JsonProperty
  private String type;
  @JsonProperty
  private Collection<EsColumn> data = new ArrayList<>();
  @JsonProperty
  private Collection<EsColumn> links = new ArrayList<>();
  @JsonProperty
  private Collection<EsColumn> system = new ArrayList<>();

  public ContentStructure() {
  }

  public ContentStructure(String index, String type) {
    this();
    this.index = index;
    this.type = type;
  }

  public ContentStructure addData(EsColumn column) {
    data.add(column);
    return this;
  }

  public ContentStructure addLink(EsColumn column) {
    links.add(column);
    return this;
  }

  public ContentStructure addFd(EsColumn column) {
    system.add(column);
    return this;
  }

  public String getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }


  public Collection<EsColumn> getLinks() {
    return links;
  }

  public Collection<EsColumn> getSystem() {
    return system;
  }

  public Collection<EsColumn> getData() {
    return data;
  }

}
