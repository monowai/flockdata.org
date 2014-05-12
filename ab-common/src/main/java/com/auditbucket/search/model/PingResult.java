package com.auditbucket.search.model;

public class PingResult {

    public PingResult() {
    }

    public PingResult(String message) {
        this.message = message;
    }

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
