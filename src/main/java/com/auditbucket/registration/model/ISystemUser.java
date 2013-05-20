package com.auditbucket.registration.model;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 6/04/13
 * Time: 11:02
 */


public interface ISystemUser {
    public abstract String getName();

    public abstract ICompany getCompany();
}
