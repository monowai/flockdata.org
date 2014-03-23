package com.auditbucket.helper;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/23/14
 * Time: 9:19 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Command<T> {
    public Command execute();
}
