package com.auditbucket.test.functional;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.DeleteIndex;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.Rollback;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:55 PM
 */
public class ESBase {
    private static Logger logger = LoggerFactory.getLogger(TestMappings.class);

    static Properties properties = new Properties();

    private static JestClient esClient;


    static void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./ab-search/src/test/resources/ab-search-config.properties");
        properties.load(f);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

    }

}
