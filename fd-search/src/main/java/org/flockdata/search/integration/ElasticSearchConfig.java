package org.flockdata.search.integration;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

/**
 * Created by mike on 16/02/16.
 */
@Configuration
public class ElasticSearchConfig {

    @Value("${es.cluster.name:es_flockdata}")
    String clusterName;
    @Value("${es.path.home:.}")
    String pathHome;
    @Value("${es.path.data:/data/es}")
    String pathData;
    @Value("${es.http.port:9200}")
    String httpPort;
    @Value("${es.transport.tcp.port:9300}")
    String tcpPort;

    private Client client;

    private Settings getSettings() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
                .put("clustername", clusterName)
                .put("path.home", pathHome)
                .put("path.data", pathHome+pathData)
                .put("http.port", httpPort)
                .put("transport.tcp.port", tcpPort);
        return settings.build();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchTemplate(elasticSearchClient());
    }

    public Client elasticSearchClient(){
        if ( client == null )
            client = esNode().client();
        return client;
//        return esNode().client();
    }

    Node esNode(){
        return  org.elasticsearch.node.NodeBuilder
                .nodeBuilder()
                .settings(getSettings())
                .clusterName(clusterName)
                .local(true)
                .node();
    }

}
