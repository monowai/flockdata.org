package com.auditbucket.registration.model;

public interface IFortressUser {

	public abstract String getName();

	public abstract void setName(String name);
	
	public IFortress getFortress ();
	
	public void setFortress (IFortress fortress);


    public Long getId();
}