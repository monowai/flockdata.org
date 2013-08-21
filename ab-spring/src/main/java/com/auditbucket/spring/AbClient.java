package com.auditbucket.spring;

public class AbClient {
     private String serverName;
     private String apiKey;
     private String userName;
     private String password;
     private String forteressName;

    public AbClient(String serverName, String apiKey, String userName, String password, String forteressName) {
        this.serverName = serverName;
        this.apiKey = apiKey;
        this.userName = userName;
        this.password = password;
        this.forteressName = forteressName;
    }
}
