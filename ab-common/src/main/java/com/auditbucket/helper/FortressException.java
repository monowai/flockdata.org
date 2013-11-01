package com.auditbucket.helper;

/**
 * User: Mike Holdsworth
 * Since: 1/11/13
 */
public class FortressException extends Exception {
    public FortressException() {
    }

    public FortressException(String message) {
        super(message);
    }

    public FortressException(String message, Throwable t) {
        super(message, t);
    }
}
