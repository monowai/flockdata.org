package com.auditbucket.engine.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.auditbucket.engine.service.SchemaService;
import com.auditbucket.registration.event.NewCompanyEvent;
import com.auditbucket.registration.model.Company;

@Service
public class NewCompanyEventHandler implements
		ApplicationListener<NewCompanyEvent> {
	private Logger logger = LoggerFactory
			.getLogger(NewCompanyEventHandler.class);

	SchemaService schemaService;

	public SchemaService getSchemaService() {
		return schemaService;
	}

	public void setSchemaService(SchemaService schemaService) {
		this.schemaService = schemaService;
	}

	@Override
	public void onApplicationEvent(NewCompanyEvent event) {
		logger.info("Handling New Company event - " + event);

		Company company = (Company) event.getSource();
//		schemaService.ensureSystemIndexes(company);
	}

}
