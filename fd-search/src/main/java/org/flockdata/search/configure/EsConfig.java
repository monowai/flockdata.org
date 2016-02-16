package org.flockdata.search.configure;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.flockdata.track.service.EntityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

/**
 * Created by mike on 16/02/16.
 */
@Configuration
public class EsConfig {

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

    @Value("${es.mappings:./}")
    private String esMappingPath;

    @Value("${es.settings:/fd-default-settings.json}")
    String esSettings;

    String esDefaultMapping = "fd-default-mapping.json";
    String esTaxonomyMapping = "fd-taxonomy-mapping.json"; // ToDo: hard coded taxonmy is not very flexible!

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

    public String getEsMappingPath() {

        if (esMappingPath.equals("${es.mappings}"))
            esMappingPath = "/"; // Internal
        return esMappingPath;
    }

    public String getEsDefaultSettings() {
        if (esSettings.equals("${es.settings}"))
            esSettings = "/fd-default-settings.json";
        return esSettings;
    }
    public String getEsMapping(EntityService.TAG_STRUCTURE tagStructure) {
        if (tagStructure != null && tagStructure == EntityService.TAG_STRUCTURE.TAXONOMY)
            return "/" + esTaxonomyMapping;
        else
            return "/" + esDefaultMapping;
    }

    public String getEsPathedMapping(EntityService.TAG_STRUCTURE tagStructure) {
        return getEsMappingPath() + getEsMapping(tagStructure);
    }





}
