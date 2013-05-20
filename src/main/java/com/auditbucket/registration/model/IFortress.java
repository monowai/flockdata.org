package com.auditbucket.registration.model;


import java.util.UUID;

public interface IFortress {

    public abstract Long getId();

    public abstract String getName();

	public abstract void setName(String name);

	public abstract ICompany getCompany();
	
	public abstract void setCompany(ICompany ownedBy);

    public UUID getUUID();

    public String getRepo();

    public String getUsers();
}