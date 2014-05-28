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

package com.auditbucket.client.rest;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.*;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Template to support writing Audit and Tag information to a remote AuditBucket instance.
 *
 * @see com.auditbucket.client.Importer
 *      <p/>
 *      User: Mike Holdsworth
 *      Since: 13/10/13
 */
public class AbRestClient implements StaticDataResolver {

    private String NEW_HEADER;
    private String NEW_TAG;
    private String CROSS_REFERENCES;
    private String FORTRESS;
    private String COUNTRIES;
    private String PING;
    private String HEALTH;
    private String REGISTER;
    private final String userName;
    private final String password;
    private final String apiKey;
    private int batchSize;
    private static boolean compress = true;
    private boolean simulateOnly;
    private List<MetaInputBean> batchHeader = new ArrayList<>();
    private Map<String, TagInputBean> batchTag = new HashMap<>();
    private final String headerSync = "BatchSync";
    private final String tagSync = "TagSync";
    private String defaultFortress;

    /**
     * Use this version for administrative access where the username and password must exist
     *
     * @param serverName   where are we talking to?
     * @param userName     configured user in the security domain
     * @param password     configured password in the security domain
     * @param batchSize    default batch command size
     */
    public AbRestClient(String serverName, String userName, String password, int batchSize) {
        this(serverName, null, userName, password, batchSize, null);
    }

    public AbRestClient(String serverName, String apiKey, String userName, String password, int batchSize, String defaultFortress) {
        headers = null;
        this.userName = userName;
        this.password = password;
        this.apiKey = apiKey;
        // Urls to write Audit/Tag/Fortress information
        this.NEW_HEADER = serverName + "/v1/track/";
        this.PING = serverName + "/v1/admin/ping/";
        this.REGISTER = serverName + "/v1/profiles/";
        this.HEALTH = serverName + "/v1/admin/health/";
        this.CROSS_REFERENCES = serverName + "/v1/track/xref/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.COUNTRIES = serverName + "/v1/geo/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
    }

    public AbRestClient(String serverName, String apiKey, int i) {
        this(serverName, apiKey, null, null, i, null);

    }

    public String ping() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = getHeaders(apiKey, null, null);// Unauthorized ping is ok
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<String> response = restTemplate.exchange(PING, HttpMethod.GET, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401"))
                return "auth";
            else
                return e.getMessage();
        } catch (HttpServerErrorException e) {
            return "err";
        } catch (ResourceAccessException e) {
            return "err";
        }

    }

    public Map<String, Object> health() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        Map<String, Object> result = new HashMap<>();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(HEALTH, HttpMethod.GET, requestEntity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401")) {
                result.put("error", "auth");
            } else {
                result.put("error", e.getMessage());
            }

        } catch (HttpServerErrorException | ResourceAccessException e) {
            result.put("error", e.getMessage());
        }
        return result;

    }

    public boolean isSimulateOnly() {
        return simulateOnly;
    }

    public SystemUserResultBean registerProfile(String authUser, String authPass, String userName, String company) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = getHeaders(null, authUser, authPass); // Internal application authorisation
        RegistrationBean registrationBean = new RegistrationBean(company, userName).setIsUnique(false);
        HttpEntity requestEntity = new HttpEntity<>(registrationBean, httpHeaders);

        try {
            ResponseEntity<SystemUserResultBean> response = restTemplate.exchange(REGISTER, HttpMethod.POST, requestEntity, SystemUserResultBean.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            return null;
        } catch (HttpServerErrorException e) {
            return null;
        } catch (ResourceAccessException e) {
            return null;
        }

    }

    public enum type {TRACK, TAG}

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AbRestClient.class);

    public void setSimulateOnly(boolean simulateOnly) {
        this.simulateOnly = simulateOnly;
    }

    public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws DatagioException{
        logger.info("Processing [{}] cross references - simulate [{}]", referenceInputBeans.size(), simulateOnly);
        if (simulateOnly)
            return 0;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<CrossReferenceInputBean>> requestEntity = new HttpEntity<>(referenceInputBeans, httpHeaders);
        try {
            ResponseEntity<ArrayList> response = restTemplate.exchange(CROSS_REFERENCES, HttpMethod.POST, requestEntity, ArrayList.class);
            logServerMessages(response);
            return referenceInputBeans.size();
        } catch (HttpClientErrorException e) {
            logger.error("AB Client Audit error {}", getErrorMessage(e));
            return 0;
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", getErrorMessage(e));
            return 0;

        }

    }

    public Collection<TagInputBean> getCountries() throws DatagioException{
        if (simulateOnly)
            return null;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<TagInputBean>> requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<String> response = restTemplate.exchange(COUNTRIES, HttpMethod.GET, requestEntity, String.class);
            TypeFactory typeFactory = mapper.getTypeFactory();
            CollectionType collectionType = typeFactory.constructCollectionType(ArrayList.class, TagInputBean.class);
            return mapper.readValue(response.getBody(), collectionType);
        } catch (HttpClientErrorException e) {
            // ToDo: Rest error handling pretty useless. need to know why it's failing
            logger.error("AB Client Audit error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", getErrorMessage(e));
            return null;

        } catch (JsonMappingException | JsonParseException e) {
            logger.error("Unexpected", e);
        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
        return null;
    }

    Map<String, TagInputBean> countriesByName = null;

    /**
     * resolves the country Name to an ISO code
     *
     * @param name long name of the country
     * @return iso code to use
     */
    @Override
    public String resolveCountryISOFromName(String name) throws DatagioException{
        if (simulateOnly)
            return name;

        // 2 char country? it's already ISO
        if ( name.length()==2)
            return name;

        if (countriesByName == null) {

            Collection<TagInputBean> countries = getCountries();
            countriesByName = new HashMap<>(countries.size());
            for (TagInputBean next : countries) {
                countriesByName.put(next.getName().toLowerCase(), next);
            }
        }
        TagInputBean tag = countriesByName.get(name.toLowerCase());
        if (tag == null) {
            logger.error("Unable to resolve country name [{}]", name);
            return null;
        }
        return tag.getCode();
    }


    private String flushAudit(List<MetaInputBean> auditInput) throws DatagioException{
        if (simulateOnly || auditInput.isEmpty())
            return "OK";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
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

    public String flushTags(List<TagInputBean> tagInputBean) throws DatagioException{
        if (tagInputBean.isEmpty())
            return "OK";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<TagInputBean>> requestEntity = new HttpEntity<>(tagInputBean, httpHeaders);

        try {
            // ToDo logServerMessage - error state will be returned in arraylist
            ResponseEntity<ArrayList> response = restTemplate.exchange(NEW_TAG, HttpMethod.PUT, requestEntity, ArrayList.class);
            logServerMessages(response);
            return "OK";
        } catch (HttpClientErrorException e) {
            // to test, try to log against no existing fortress.
            logger.error("Datagio client error processing Tags {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("Datagio server error processing Tags {}", getErrorMessage(e));
            return null;

        }
    }

    private void logServerMessages(ResponseEntity<ArrayList> response) {
        ArrayList x = response.getBody();
        for (Object val : x) {
            Map map = (Map) val;
            Object serviceMessage = map.get("serviceMessage");
            if (serviceMessage != null)
                logger.error("Service returned [{}]", serviceMessage.toString());
        }
    }

    public void ensureFortress(String fortressName) throws DatagioException{
        if (fortressName == null)
            return;

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<FortressInputBean> request = new HttpEntity<>(new FortressInputBean(fortressName), httpHeaders);
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

    public String getErrorMessage(HttpStatusCodeException e) throws DatagioException{

        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR || e.getStatusCode()== HttpStatus.BAD_REQUEST|| e.getStatusCode()== HttpStatus.BAD_REQUEST) {
            logger.error(e.getResponseBodyAsString());
            String error = e.getResponseBodyAsString();
            if ( error.contains("Invalid API"))
                logger.info("Your API key appears to be invalid. Have you run the configure process?");
            throw new DatagioException(error);
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

    public static HttpHeaders getHeaders(final String apiKey, final String username, final String password) {
        if (headers != null)
            return headers;

        headers = new HttpHeaders() {
            {
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(
                            auth.getBytes(Charset.forName("US-ASCII")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                } else if ( apiKey != null )
                    set("Api-Key", apiKey);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
                if (compress)
                    set("Accept-Encoding", "gzip,deflate");
            }
        };

        return headers;
    }

    public void flush(String message) throws DatagioException{
        flush(message, type.TAG);
        flush(message, type.TRACK);
    }

    /**
     * push any remaining updates
     */
    public void flush(String message, type abType) throws DatagioException{
        if (simulateOnly)
            return;
        if (abType.equals(type.TRACK)) {
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
    public void writeAudit(MetaInputBean metaInputBean, String message) throws DatagioException{
        writeAudit(metaInputBean, false, message);
    }

    private void batchTags(MetaInputBean metaInputBeans) {

        for (TagInputBean tag : metaInputBeans.getTags()) {
            String indexKey = tag.getCode() + tag.getIndex();
            TagInputBean cachedTag = batchTag.get(indexKey);
            if (cachedTag == null)
                batchTag.put(indexKey, tag);
            else {
                cachedTag.mergeTags(tag);
            }
        }
    }

    void writeAudit(MetaInputBean metaInputBean, boolean flush, String message) throws DatagioException{

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
                    flushTags(new ArrayList<>(batchTag.values()));
                    flushAudit(batchHeader);
                    logger.debug("Flushed " + message + " Batch [{}]", batchHeader.size());
                }
                batchHeader = new ArrayList<>();
                batchTag = new HashMap<>();
            }

        }

    }

    public void writeTag(TagInputBean tagInputBean, String message) throws DatagioException{
        writeTag(tagInputBean, false, message);
    }

    private void writeTag(TagInputBean tagInputBean, boolean flush, String message) throws DatagioException{

        synchronized (tagSync) {
            if (tagInputBean != null)
                batchTag.put(tagInputBean.getName() + tagInputBean.getIndex(), tagInputBean);

            if (flush || batchTag.size() == batchSize) {
                logger.debug("Flushing " + message + " Tag Batch [{}]", batchTag.size());
                if (batchTag.size() >= 0)
                    flushTags(new ArrayList<>(batchTag.values()));
                logger.debug("Tag Batch Flushed");
                batchTag = new HashMap<>();
            }
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
            String header = headerRow[i];

            if (header.startsWith("@!"))
                header = headerRow[i].substring(2, headerRow[i].length());
            else if (header.startsWith("@") || header.startsWith("$") || header.startsWith("*"))
                header = headerRow[i].substring(1, headerRow[i].length());

            node.put(header, line[i].trim());
        }
        return node.toString();
    }

}
