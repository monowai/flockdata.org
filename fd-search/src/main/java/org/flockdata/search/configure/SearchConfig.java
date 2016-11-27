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

package org.flockdata.search.configure;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author mholdsworth
 * @since 16/02/2016
 * @tag Search, Configuration
 */
@Configuration
public class SearchConfig {

    @Value("${es.clustername:es_flockdata}")
    String clusterName;
    @Value("${es.path.home}:./")
    String pathHome;
    @Value("${es.path.data:${es.path.home}/data/es}")
    String pathData;
    @Value("${es.http.port:9200}")
    String httpPort;
    @Value("${es.tcp.port:9300}")
    String tcpPort;

    @Value("${org.fd.search.es.transportOnly:false}")
    Boolean transportOnly;
    @Value("${org.fd.search.es.local:true}")
    Boolean localOnly;
    @Value("${org.fd.search.es.settings:fd-default-settings.json}")
    String esSettings;
    @Value("${org.fd.search.es.mappings:'.'}")
    private String esMappingPath;
    private InetSocketTransportAddress[] addresses;
    private String urls;


    private String esDefaultMapping = "fd-default-mapping.json";
    private String esTaxonomyMapping = "fd-taxonomy-mapping.json"; // ToDo: hard coded taxonmy is not very flexible!

    private Client client;

    private Logger logger = LoggerFactory.getLogger("configuration");

    private Settings getSettings() {
        Settings settings = Settings.settingsBuilder()
                .put("path.data", pathData)
                .put("path.home", pathHome)
                .put("http.port", httpPort)
                .put("cluster.name", clusterName)
                .put("node.local", (!transportOnly ? false : localOnly))
                .put("node.client", transportOnly)
                .put("transport.tcp.port", tcpPort).build();


        logger.info("ElasticSearch config settings " + JsonUtils.toJson(settings.getAsMap()));
        return settings;
    }

//    @Bean
//    JestClient getJestClient (){
//        JestClientFactory factory = new JestClientFactory();
//        factory.setHttpClientConfig(new HttpClientConfig
//                .Builder(urls)
//                .multiThreaded(true)
//                .build());
//        return factory.getObject();
//
//
//    }
//
//    @Bean
//    public ElasticsearchOperations elasticsearchTemplate() throws UnknownHostException {
//        return new ElasticsearchTemplate(elasticSearchClient());
//    }

    @PreDestroy
    void closeClient() {
        if (client != null)
            client.close();
    }

    @Bean
    public Client elasticSearchClient() {
        if (client == null) {
            if (transportOnly) {
                // You should set the host addresses to connect to
                for (InetSocketTransportAddress address : addresses) {
                    logger.info("**** Transport client looking for host {}", address.toString());
                }
                client = TransportClient
                        .builder()
                            .settings(getSettings())
                        .build()
                            .addTransportAddresses((TransportAddress[]) addresses);

            } else {
                logger.info("Using embedded ES node");
                // Embedded node
                client = esNode()
                        .client();
            }
        }

        return client;
    }

    public String getTransportAddresses() {
        if (!transportOnly)
            return null;
        String result = null;
        for (InetSocketTransportAddress address : addresses) {
            if (result != null)
                result = result + "," + address.toString();
            else
                result = address.toString();
        }
        return result;
    }

    /**
     * Transport hosts
     * <p>
     * HostA:9300,HostB:9300,HostC:9300.....
     *
     * @param urls , separated list of hosts to connect to
     */
    @Autowired
    void setTransportAddresses(@Value("${es.nodes:localhost:9300}") String urls) throws UnknownHostException {
        Collection<String> values;
        if (urls == null || urls.equals("")) {
            return;
        }
        this.urls = urls;
        values = Arrays.asList(urls.split(","));

        addresses = new InetSocketTransportAddress[values.size()];
        int i = 0;
        for (String value : values) {
            String[] serverPort = value.split(":");
            addresses[i++] = new InetSocketTransportAddress(InetAddress.getByName(serverPort[0]), Integer.parseInt(serverPort[1]));
        }
    }

    Node esNode() {
        return org.elasticsearch.node.NodeBuilder
                .nodeBuilder()
                .settings(getSettings())
                .clusterName(clusterName)
                .local(!transportOnly ? false : localOnly)
                .client(transportOnly)
                .node();
    }

    public String getEsMappingPath() {

        if (esMappingPath.equals("${es.mappings}"))
            esMappingPath = "."; // Internal
        return esMappingPath;
    }

    public String getEsDefaultSettings() {
        if (esSettings.equals("${es.settings}"))
            esSettings = "fd-default-settings.json";
        return esSettings;
    }

    public String getEsMapping(SearchChange searchChange) {
        if (searchChange.isType(SearchChange.Type.ENTITY)) {
            if (searchChange.getTagStructure() != null && searchChange.getTagStructure() == EntityService.TAG_STRUCTURE.TAXONOMY)
                return esTaxonomyMapping;
            else
                return esDefaultMapping;
        } else {
            return "fd-tag-mapping.json";
        }
    }

    public String getEsPathedMapping(SearchChange tagStructure) {
        return getEsMappingPath() + getEsMapping(tagStructure);
    }

}
