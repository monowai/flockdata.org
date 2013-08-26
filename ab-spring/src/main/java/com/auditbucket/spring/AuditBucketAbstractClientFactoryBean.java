package com.auditbucket.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


public abstract class AuditBucketAbstractClientFactoryBean extends AuditBucketAbstractFactoryBean
        implements FactoryBean<AbClient>, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    protected AbClient client;

    /**
     * Implement this method to build an AuditBucket client
     *
     * @return AuditBucket Client
     * @throws Exception if something goes wrong
     */
    abstract protected AbClient buildClient() throws Exception;


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
                //client.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing AuditBucket client: ", e);
        }
    }

    @Override
    public AbClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<AbClient> getObjectType() {
        return AbClient.class;
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
