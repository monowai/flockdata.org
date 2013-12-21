package com.auditbucket.spring;

import com.auditbucket.helper.AbExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuditBucketClientFactoryBean extends AuditBucketAbstractClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    protected AbExporter buildClient() throws Exception {
        return new AbExporter(properties.get("server.name").toString(),
                properties.get("ab.username").toString(),
                properties.get("ab.password").toString(), 1);

    }
}
