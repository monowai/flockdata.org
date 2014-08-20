package com.auditbucket.registration.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

import com.auditbucket.registration.event.NewCompanyEvent;
import com.auditbucket.registration.model.Company;

@Service
public class NewCompanyEventPublisher implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	public void publish(Company company) {
		NewCompanyEvent newCompanyEvent = new NewCompanyEvent(company);
		this.applicationEventPublisher.publishEvent(newCompanyEvent);
	}

}
