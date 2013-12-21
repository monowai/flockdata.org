package com.auditbucket.spring;

import com.auditbucket.helper.AbExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


abstract class AuditBucketAbstractClientFactoryBean extends AuditBucketAbstractFactoryBean
        implements FactoryBean<AbExporter>, InitializingBean, DisposableBean {

    private final Log logger = LogFactory.getLog(getClass());

    private AbExporter client;

    /**
     * Implement this method to build an AuditBucket client
     *
     * @return AuditBucket Client
     * @throws Exception if something goes wrong
     */
    abstract protected AbExporter buildClient() throws Exception;


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
    public AbExporter getObject() throws Exception {
        return client;
    }

    @Override
    public Class<AbExporter> getObjectType() {
        return AbExporter.class;
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
