package com.auditbucket.registration.event;

import org.springframework.context.ApplicationEvent;

public class NewCompanyEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5681205101025236823L;

	public NewCompanyEvent(Object source) {
		super(source);
	}

	@Override
	public String toString() {
		return "CompanyCreatedEvent [source=" + source + "]";
	}

}
