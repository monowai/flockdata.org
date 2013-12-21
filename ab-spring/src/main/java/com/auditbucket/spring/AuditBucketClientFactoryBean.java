package com.auditbucket.spring;

import com.auditbucket.helper.AbExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuditBucketClientFactoryBean extends AuditBucketAbstractClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    protected AbExporter buildClient() throws Exception {
        Object f = properties.get("ab.fortress");
        String fortressName = null;
        if (f != null)
            fortressName = f.toString();
        Object b = properties.get("ab.batch");
        Integer batchSize = null;
        if (b != null)
            batchSize = Integer.parseInt(b.toString());
        else
            batchSize = Integer.parseInt("1");
        AbExporter exporter = new AbExporter(properties.get("server.name").toString(),
                properties.get("ab.username").toString(),
                properties.get("ab.password").toString(),
                batchSize,
                fortressName
        );
        exporter.setSimulateOnly((batchSize.intValue() <= 0));
        exporter.ensureFortress(fortressName);
        return exporter;

    }
}
