package com.auditbucket.spring;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.spring.utils.PojoToAbTransformer;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

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
        auditHeaderInputBean.setFortress(forteressName);
        auditHeaderInputBean.setFortressUser(userName);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = createHeaders(userName, password);
        HttpEntity<AuditHeaderInputBean> requestEntity = new HttpEntity<AuditHeaderInputBean>(auditHeaderInputBean, httpHeaders);

        ResponseEntity response = restTemplate.exchange(serverName + "/audit/header/new", HttpMethod.POST, requestEntity, AuditResultBean.class);
        // TODO dependeing on  response.getStatusCode() we must throw or not a specific AB exception
        return (AuditResultBean) response.getBody();
    }

    public AuditLogInputBean createLogHeader(Object pojo) throws IllegalAccessException {
        AuditLogInputBean auditLogInputBean = PojoToAbTransformer.transformToAbLogFormat(pojo);
        HttpHeaders httpHeaders = createHeaders(userName, password);
        HttpEntity<AuditLogInputBean> requestEntity = new HttpEntity<AuditLogInputBean>(auditLogInputBean, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        ResponseEntity response = restTemplate.exchange(serverName + "/audit/log/new", HttpMethod.POST, requestEntity, AuditLogInputBean.class);

        // TODO depending on  response.getStatusCode() we must throw or not a specific AB exception
        return (AuditLogInputBean) response.getBody();
    }

    private HttpHeaders createHeaders(final String username, final String password) {
        HttpHeaders headers = new HttpHeaders() {
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
            }
        };
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");

        return headers;
    }

}
