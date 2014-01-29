/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.client;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditResultBean;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.FortressResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Template to support writing Audit and Tag information to a remote AuditBucket instance.
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public class AbRestClient {

    private String NEW_HEADER;
    private String NEW_TAG;
    private String FORTRESS;
    private final String userName;
    private final String password;
    private int batchSize;
    private static boolean compress = false;
    private boolean simulateOnly;
    private List<AuditHeaderInputBean> batchHeader = new ArrayList<>();
    private List<TagInputBean> batchTag = new ArrayList<>();
    private final String batchSync = "BatchSync";
    private final String tagSync = "TagSync";
    private String defaultFortress;

    public void setSimulateOnly(boolean simulateOnly) {
        this.simulateOnly = simulateOnly;
    }

    public enum type {AUDIT, TAG}

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AbRestClient.class);

    public AbRestClient(String serverName, String userName, String password, int batchSize) {
        this(serverName, userName, password, batchSize, null);
    }

    public AbRestClient(String serverName, String userName, String password, int batchSize, String defaultFortress) {
        this.userName = userName;
        this.password = password;
        this.NEW_HEADER = serverName + "/v1/audit/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
    }

    /**
     * creates a rating value for a relationship
     *
     * @param relationshipName from Tag to AuditHeader
     * @param weight           - weight of the rating.
     * @return constructed Tag Map that AuditBucket will handle
     */
    public static Map<String, Object> getWeightedRelationship(String relationshipName, int weight) {
        Map<String, Object> relationship = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        relationship.put(relationshipName, properties);
        properties.put("weight", weight);
        return relationship;
    }

    public static Map<String, Object> getWeightedMap(int weight) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("weight", weight);
        return properties;
    }

    public void ensureFortress(String fortressName) {
        if (fortressName == null)
            return;

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(userName, password);
        HttpEntity<FortressInputBean> request = new HttpEntity<>(new FortressInputBean(fortressName, false), httpHeaders);
        try {
            restTemplate.exchange(FORTRESS, HttpMethod.POST, request, FortressResultBean.class);
            if (defaultFortress != null && !defaultFortress.equals(fortressName)) {
                request = new HttpEntity<>(new FortressInputBean(defaultFortress, false), httpHeaders);
                restTemplate.exchange(FORTRESS, HttpMethod.POST, request, FortressResultBean.class);
            }
        } catch (HttpClientErrorException e) {
            // ToDo: Rest error handling pretty useless. need to know why it's failing
            logger.error("AB Client Audit error {}", getErrorMessage(e));
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", getErrorMessage(e));

        }


    }

    private String flushAudit(AuditHeaderInputBean[] auditInput) {
        if (simulateOnly)
            return "OK";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(userName, password);
        HttpEntity<AuditHeaderInputBean[]> requestEntity = new HttpEntity<>(auditInput, httpHeaders);

        try {
            restTemplate.exchange(NEW_HEADER, HttpMethod.PUT, requestEntity, AuditResultBean.class);
            return "OK";
        } catch (HttpClientErrorException e) {
            // ToDo: Rest error handling pretty useless. need to know why it's failing
            logger.error("AB Client Audit error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", getErrorMessage(e));
            return null;

        }
    }

    public String getErrorMessage(HttpStatusCodeException e) {

        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.error(e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }

        JsonNode n = null;
        try {
            n = mapper.readTree(e.getResponseBodyAsByteArray());
        } catch (IOException e1) {

            logger.error(String.valueOf(e1));
        }
        String message;
        if (n != null)
            message = String.valueOf(n.get("message"));
        else
            message = e.getMessage();

        return message;
    }

    private static HttpHeaders headers = null;

    private static HttpHeaders getHeaders(final String username, final String password) {
        if (headers != null)
            return headers;

        headers = new HttpHeaders() {
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
                if (compress)
                    set("Accept-Encoding", "gzip,deflate");
            }
        };

        return headers;
    }

    static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts the strings to a simple JSON representation
     *
     * @param headerRow - keys
     * @param line      - values
     * @return JSON Object
     * @throws JsonProcessingException
     */
    public static String convertToJson(String[] headerRow, String[] line) throws JsonProcessingException {
        ObjectNode node = mapper.createObjectNode();
        for (int i = 0; i < headerRow.length; i++) {
            node.put(headerRow[i], line[i].trim());
        }
        return node.toString();
    }

    public void flush(String message) {
        flush(message, type.TAG);
        flush(message, type.AUDIT);
    }

    /**
     * push any remaining updates
     */
    public void flush(String message, type abType) {
        if (simulateOnly)
            return;
        if (abType.equals(type.AUDIT)) {
            synchronized (batchSync) {
                writeAudit(null, true, message);
            }
        } else {
            synchronized (tagSync) {
                writeTag(null, true, message);
            }
        }
    }

    /**
     * send the data to AuditBucket
     *
     * @param auditHeaderInputBean Input to push
     */
    public void writeAudit(AuditHeaderInputBean auditHeaderInputBean, String message) {
        writeAudit(auditHeaderInputBean, false, message);
    }

    void writeAudit(AuditHeaderInputBean auditHeaderInputBean, boolean flush, String message) {

        synchronized (batchSync) {
            if (auditHeaderInputBean != null) {
                if (auditHeaderInputBean.getFortress() == null)
                    auditHeaderInputBean.setFortress(defaultFortress);
                batchHeader.add(auditHeaderInputBean);
            }

            if (flush || batchHeader.size() == batchSize) {
                AuditHeaderInputBean[] thisBatch = new AuditHeaderInputBean[batchHeader.size()];
                ListIterator<AuditHeaderInputBean> it = batchHeader.listIterator();
                int i = 0;
                while (it.hasNext()) {
                    thisBatch[i] = it.next();
                    i++;
                }

                if (i > 1) {
                    logger.debug("Flushing....");
                    flushAudit(thisBatch);
                    logger.debug("Flushed " + message + " Batch [{}]", i);
                }
                batchHeader = new ArrayList<>();
            }
        }

    }

    public void writeTag(TagInputBean tagInputBean, String message) {
        writeTag(tagInputBean, false, message);
    }

    void writeTag(TagInputBean tagInputBean, boolean flush, String message) {

        synchronized (tagSync) {
            if (tagInputBean != null)
                batchTag.add(tagInputBean);

            if (flush || batchTag.size() == batchSize) {
                TagInputBean[] thisBatch = new TagInputBean[batchTag.size()];
                ListIterator<TagInputBean> it = batchTag.listIterator();
                int i = 0;
                while (it.hasNext()) {
                    thisBatch[i] = it.next();
                    i++;
                }
                logger.debug("Flushing " + message + " Tag Batch [{}]", i);
                if (i > 1)
                    flushTags(thisBatch);
                logger.debug("Tag Batch Flushed");
                batchHeader = new ArrayList<>();
            }
        }

    }

    public String flushTags(TagInputBean[] tagInputBean) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(userName, password);
        HttpEntity<TagInputBean[]> requestEntity = new HttpEntity<>(tagInputBean, httpHeaders);

        //logger.info("template {}", restTemplate);
        try {
            restTemplate.exchange(NEW_TAG, HttpMethod.PUT, requestEntity, AuditResultBean.class);
            return "OK";
        } catch (HttpClientErrorException e) {
            // to test, try to log against no existing fortress.
            logger.error("AB Client Tag error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Tag error {}", getErrorMessage(e));
            return null;

        }
    }


}
