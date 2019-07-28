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

package org.flockdata.search.configure;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.flockdata.data.EntityTag;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.track.bean.SearchChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * Encapsulate configuration of an ES client and transport infrastructure
 *
 * @author mholdsworth
 * @tag Search, Configuration
 * @since 16/02/2016
 */
@Configuration
@Service
public class SearchConfig {

  @Value("${es.clustername:es_flockdata}")
  private String clusterName;

  @Value("${es.http.port:9200}")
  private Integer httpPort;

  @Value("${es.http.host:127.0.0.1}")
  private String httpHost;

  @Value("${es.tcp.port:9300}")
  private Integer tcpPort;

  @Value("${org.fd.search.es.transportOnly:true}")
  private Boolean transportOnly = true;

  @Value("${org.fd.search.es.settings:fd-default-settings.json}")
  private String esSettings;
  @Value("${org.fd.search.es.mappings:'.'}")
  private String esMappingPath;
  private InetSocketTransportAddress[] addresses;

  private Client client;
  private RestClient restClient;
  private RestHighLevelClient restHighLevelClient;
  private IndexManager indexManager;

  private Logger logger = LoggerFactory.getLogger("configuration");

  @Autowired
  public void initSearchConfig() {
    restClient = RestClient.builder(
        new HttpHost(httpHost, httpPort, "http")).build();
    restHighLevelClient =
        new RestHighLevelClient(restClient);

  }

  public RestHighLevelClient getRestHighLevelClient() {
    return restHighLevelClient;
  }

  @Deprecated
  public Client getClient() {
    return client;
  }

  public IndexManager getIndexManager() {
    return indexManager;
  }

  @Autowired
  void setIndexManager(IndexManager indexManager) {
    this.indexManager = indexManager;
  }

  public String getTransportAddresses() {
    StringBuilder result = null;
    for (InetSocketTransportAddress address : addresses) {
      if (result != null) {
        result.append(",").append(address.toString());
      } else {
        result = new StringBuilder(address.toString());
      }
    }
    return result != null ? result.toString() : null;
  }

  /**
   * Transport hosts
   * <p>
   * HostA:9300,HostB:9300,HostC:9300.....
   *
   * @param urls , separated list of hosts to connect to
   */
  @Autowired
  @Deprecated
  void setTransportClient(@Value("${es.nodes:localhost:9303}") String[] urls) throws UnknownHostException {
    if (urls == null || urls.length == 0) {
      return;
    }
    addresses = new InetSocketTransportAddress[urls.length];
    int i = 0;
    for (String value : urls) {
      String[] serverPort = value.split(":");
      InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(serverPort[0]), Integer.parseInt(serverPort[1]));
      addresses[i++] = address;
      logger.info("**** Transport client looking for host {}", address.toString());
    }
    Settings transportSettings;
    transportSettings = Settings.builder()
        .put("cluster.name", clusterName)
        .build();

    logger.info("ElasticSearch config settings " + JsonUtils.toJson(transportSettings.getAsMap()));

    this.client = new PreBuiltTransportClient(transportSettings)
        .addTransportAddresses((TransportAddress[]) addresses);

  }

  public String getEsMappingPath() {

    if (esMappingPath.equals("${es.mappings}")) {
      esMappingPath = "."; // Internal
    }
    return esMappingPath;
  }

  public String getEsDefaultSettings() {
    if (esSettings.equals("${es.settings}")) {
      esSettings = "fd-default-settings.json";
    }
    return esSettings;
  }

  public String getEsMapping(SearchChange searchChange) {
    if (searchChange.isType(SearchChange.Type.ENTITY)) {
      String esDefaultMapping = "fd-default-mapping.json";
      String esTaxonomyMapping = "fd-taxonomy-mapping.json";
      if (searchChange.getTagStructure() != null && searchChange.getTagStructure() == EntityTag.TAG_STRUCTURE.TAXONOMY) {
        return esTaxonomyMapping;
      } else {
        return esDefaultMapping;
      }
    } else {
      return "fd-tag-mapping.json";
    }
  }

  public String getEsPathedMapping(SearchChange tagStructure) {
    return getEsMappingPath() + getEsMapping(tagStructure);
  }

  @PreDestroy
  void closeClients() throws IOException {
    if (restClient != null) {
      restClient.close();
      restHighLevelClient = null;
    }
    if (client != null) {
      client.close();
    }
  }


}
