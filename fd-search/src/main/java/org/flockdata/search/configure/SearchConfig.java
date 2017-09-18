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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.flockdata.data.EntityTag;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.SearchChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

/**
 * Encapsulate configuration of an ES client and transport infrastructure
 * 
 * @author mholdsworth
 * @since 16/02/2016
 * @tag Search, Configuration
 */
@Configuration
@Service
public class SearchConfig {

    @Value("${es.clustername:es_flockdata}")
    private String clusterName;
    @Value("${es.path.home}:./")
    private String pathHome;
    @Value("${es.path.data:${es.path.home}/data/es}")
    private String pathData;
    @Value("${es.http.port:9200}")
    private String httpPort;
    @Value("${es.tcp.port:9300}")
    private String tcpPort;

    @Value("${org.fd.search.es.transportOnly:true}")
    private Boolean transportOnly=false;

    @Value("${org.fd.search.es.settings:fd-default-settings.json}")
    private String esSettings;
    @Value("${org.fd.search.es.mappings:'.'}")
    private String esMappingPath;
    private InetSocketTransportAddress[] addresses;

    private Client client;

    private Logger logger = LoggerFactory.getLogger("configuration");

    @PreDestroy
    void closeClient() {
        if (client != null)
            client.close();
    }

    @Bean
    Settings getEsSettings() {
        Settings settings;
        if ( transportOnly )
            settings = Settings.builder()
                .put("cluster.name", clusterName)
                .build();

        else
            settings = Settings.builder()
                    .put("path.data", pathData)
                    .put("path.home", pathHome)
                    .put("http.port", httpPort)
                    .put("cluster.name", clusterName)
                    .put("transport.tcp.port", tcpPort).build();

        logger.info("ElasticSearch config settings " + JsonUtils.toJson(settings.getAsMap()));
        return settings;
    }

    @Bean
    public Client elasticSearchClient(Settings esSettings) throws NodeValidationException {
        if (client == null) {
            if ( transportOnly) {
                client = new PreBuiltTransportClient(esSettings)
                            .addTransportAddresses((TransportAddress[]) addresses);

            } else {
                //https://discuss.elastic.co/t/unsupported-http-type-netty3-when-trying-to-start-embedded-elasticsearch-node/69669/8
                logger.info("Using deprecated embedded ES node. Development only");
                // Embedded node
                Collection plugins = Collections.singletonList(Netty4Plugin.class);
                Node node = new PluginConfigurableNode(esSettings, plugins).start();

                try {
                    node.start();
                } catch (NodeValidationException e) {
                    throw new RuntimeException(e);
                }

                client = node
                        .client();
            }
        }

        return client;
    }

    public String getTransportAddresses() {
        if (!transportOnly)
            return null;
        StringBuilder result = null;
        for (InetSocketTransportAddress address : addresses) {
            if (result != null)
                result.append(",").append(address.toString());
            else
                result = new StringBuilder(address.toString());
        }
        return result != null ? result.toString() : null;
    }
    private static class PluginConfigurableNode extends Node {
        public PluginConfigurableNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(settings, null), classpathPlugins);
        }
    }

    /**
     * Transport hosts
     *
     * HostA:9300,HostB:9300,HostC:9300.....
     *
     * @param urls , separated list of hosts to connect to
     */
    @Autowired
    void setTransportAddresses(@Value("${es.nodes:localhost:9300}") String[] urls) throws UnknownHostException {
        if (urls == null || urls.length== 0) {
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
            String esDefaultMapping = "fd-default-mapping.json";
            String esTaxonomyMapping = "fd-taxonomy-mapping.json";
            if (searchChange.getTagStructure() != null && searchChange.getTagStructure() == EntityTag.TAG_STRUCTURE.TAXONOMY)
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
