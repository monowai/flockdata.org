package com.auditbucket.helper;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 7/03/14
 * Time: 6:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class TagException extends RuntimeException {
    public TagException() {
    }

    public TagException(String message) {
        super(message);
    }

    public TagException(String message, Throwable t) {
        super(message, t);
    }
}
