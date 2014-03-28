/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.bean.TrackResultBean;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Template to support writing Audit and Tag information to a remote AuditBucket instance.
 *
 * @see Importer
 *      <p/>
 *      User: Mike Holdsworth
 *      Since: 13/10/13
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
    private List<MetaInputBean> batchHeader = new ArrayList<>();
    private List<TagInputBean> batchTag = new ArrayList<>();
    private final String headerSync = "BatchSync";
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
        // Urls to write Audit/Tag/Fortress information
        this.NEW_HEADER = serverName + "/v1/track/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
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

    private String flushAudit(List<MetaInputBean> auditInput) {
        if (simulateOnly)
            return "OK";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(userName, password);
        HttpEntity<List<MetaInputBean>> requestEntity = new HttpEntity<>(auditInput, httpHeaders);

        try {
            restTemplate.exchange(NEW_HEADER, HttpMethod.PUT, requestEntity, TrackResultBean.class);
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
            synchronized (headerSync) {
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
     * @param metaInputBean Input to push
     */
    public void writeAudit(MetaInputBean metaInputBean, String message) {
        writeAudit(metaInputBean, false, message);
    }

    private void batchTags(MetaInputBean metaInputBeans) {

        for (TagInputBean tag : metaInputBeans.getTags()) {
            if (!batchTag.contains(tag))
                batchTag.add(tag);
        }
    }


    void writeAudit(MetaInputBean metaInputBean, boolean flush, String message) {

        synchronized (headerSync) {
            if (metaInputBean != null) {
                if (metaInputBean.getFortress() == null)
                    metaInputBean.setFortress(defaultFortress);
                batchHeader.add(metaInputBean);
                batchTags(metaInputBean);
            }

            if (flush || batchHeader.size() == batchSize) {

                if (batchHeader.size() >= 1) {
                    logger.debug("Flushing....");
                    // process the tags independently to reduce the chance of a deadlock when processing the header
                    flushTags(batchTag);
                    flushAudit(batchHeader);
                    logger.debug("Flushed " + message + " Batch [{}]", batchHeader.size());
                }
                batchHeader = new ArrayList<>();
                batchTag = new ArrayList<>();
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
                logger.debug("Flushing " + message + " Tag Batch [{}]", batchTag.size());
                if (batchTag.size() >= 0)
                    flushTags(batchTag);
                logger.debug("Tag Batch Flushed");
                batchHeader = new ArrayList<>();
            }
        }

    }

    public String flushTags(List<TagInputBean> tagInputBean) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(userName, password);
        HttpEntity<List<TagInputBean>> requestEntity = new HttpEntity<>(tagInputBean, httpHeaders);

        //logger.info("template {}", restTemplate);
        try {
            restTemplate.exchange(NEW_TAG, HttpMethod.PUT, requestEntity, TrackResultBean.class);
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

    static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> getWeightedMap(int weight) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("weight", weight);
        return properties;
    }


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

}
