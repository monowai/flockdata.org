package com.auditbucket.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

abstract class AuditBucketAbstractFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    Properties properties;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
