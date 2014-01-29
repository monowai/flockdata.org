package com.auditbucket.spring;

import com.auditbucket.client.AbRestClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


abstract class AuditBucketAbstractClientFactoryBean extends AuditBucketAbstractFactoryBean
        implements FactoryBean<AbRestClient>, InitializingBean, DisposableBean {

    private final Log logger = LogFactory.getLog(getClass());

    private AbRestClient client;

    /**
     * Implement this method to build an AuditBucket client
     *
     * @return AuditBucket Client
     * @throws Exception if something goes wrong
     */
    abstract protected AbRestClient buildClient() throws Exception;


    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Starting AuditBucket client");
        client = buildClient();
    }

    @Override
    public void destroy() throws Exception {
        try {
            logger.info("Closing AuditBucket client");
            if (client != null) {
                client.flush("");
                //client.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing AuditBucket client: ", e);
        }
    }

    @Override
    public AbRestClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<AbRestClient> getObjectType() {
        return AbRestClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Check if client is still here !
     *
     * @throws Exception
     */
    private void checkClient() throws Exception {
        if (client == null) {
            throw new Exception("AuditBucket client doesn't exist. Your factory is not properly initialized.");
        }
    }

}
