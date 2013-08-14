package com.auditbucket.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuditBucketClientFactoryBean extends AuditBucketAbstractClientFactoryBean {

	protected final Log logger = LogFactory.getLog(getClass());
	
	@Override
	protected AbClient buildClient() throws Exception {
		return null;
	}
}
