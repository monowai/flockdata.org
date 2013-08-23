package com.auditbucket.spring;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.spring.utils.PojoToAbTransformer;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

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

    public AuditResultBean createAuditHeader(Object pojo) throws IllegalAccessException {
        AuditHeaderInputBean auditHeaderInputBean = PojoToAbTransformer.transformToAbFormat(pojo);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        ResponseEntity response = restTemplate.postForObject(serverName + "/header/new", auditHeaderInputBean, ResponseEntity.class);
        // TODO dependeing on  response.getStatusCode() we must throw or not a specific AB exception
        return (AuditResultBean) response.getBody();
    }

    public AuditLogInputBean createLogHeader(Object pojo) throws IllegalAccessException {
        AuditLogInputBean auditLogInputBean = PojoToAbTransformer.transformToAbLogFormat(pojo);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        ResponseEntity response = restTemplate.postForObject(serverName + "/log/new", auditLogInputBean, ResponseEntity.class);
        // TODO depending on  response.getStatusCode() we must throw or not a specific AB exception
        return (AuditLogInputBean) response.getBody();
    }
}
