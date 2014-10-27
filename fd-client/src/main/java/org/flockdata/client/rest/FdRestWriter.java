/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.client.rest;

import org.flockdata.helper.CompressionHelper;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.transform.FdWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.codec.binary.Base64;
import org.flockdata.registration.bean.*;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Template to support writing Entity and Tag information to a remote FlockData service
 *
 * @see org.flockdata.client.Importer
 *      <p/>
 *      User: Mike Holdsworth
 *      Since: 13/10/13
 */
public class FdRestWriter implements FdWriter {

    private String NEW_ENTITY;
    private String NEW_TAG;
    private String CROSS_REFERENCES;
    private String FORTRESS;
    private String COUNTRIES;
    private String PING;
    private String AUTH_PING;
    private String ME;
    private String HEALTH;
    private String REGISTER;
    private final String userName;
    private final String password;
    private final String apiKey;
    private int batchSize;
    private static boolean compress = true;
    private boolean simulateOnly;
    private String defaultFortress;
    private ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();

    /**
     * Use this version for administrative access where the username and password must exist
     *
     * @param serverName   where are we talking to?
     * @param userName     configured user in the security domain
     * @param password     configured password in the security domain
     * @param batchSize    default batch command size
     */
    public FdRestWriter(String serverName, String userName, String password, int batchSize) {
        this(serverName, null, userName, password, batchSize, null);
    }

    public FdRestWriter(String serverName, String apiKey, String userName, String password, int batchSize, String defaultFortress) {
        httpHeaders = null;
        this.userName = userName;
        this.password = password;
        this.apiKey = apiKey;
        // Urls to write Entity/Tag/Fortress information
        this.NEW_ENTITY = serverName + "/v1/track/";
        this.AUTH_PING = serverName + "/v1/admin/ping/";
        this.PING = serverName + "/v1/ping/";
        this.REGISTER = serverName + "/v1/profiles/";
        this.ME = serverName + "/v1/profiles/me/";
        this.HEALTH = serverName + "/v1/admin/health/";
        this.CROSS_REFERENCES = serverName + "/v1/track/xref/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.COUNTRIES = serverName + "/v1/geo/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
        simulateOnly = batchSize <1;
    }

    public FdRestWriter(String serverName, String apiKey, int batchSize) {
        this(serverName, apiKey, null, null, batchSize, null);

    }

    /**
     * Helper that turns the supplied object in to a Jackson mapped Map
     * Used by fd-client
     *
     * @param o - arbitrary object
     * @return Map<String,Object>
     */
    public static Map<String,Object> convertToMap(Object o ){
        ObjectMapper om = FlockDataJsonFactory.getObjectMapper();
        return  om.convertValue(o, Map.class);
    }

    public SystemUserResultBean me(){
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);// Unauthorized ping is ok
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<SystemUserResultBean> response = restTemplate.exchange(ME, HttpMethod.GET, requestEntity, SystemUserResultBean.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401"))
                return null;
            else
                return null;
        } catch (HttpServerErrorException e) {
            return null;
        } catch (ResourceAccessException e) {
            return null;
        }


    }

    /**
     * Simple ping to see if the service is up
     *
     * @return "Pong!"
     */
    public String ping() {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders();
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

    public String pingAuth(String userName, String password) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);// Unauthorized ping is ok
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<String> response = restTemplate.exchange(AUTH_PING, HttpMethod.GET, requestEntity, String.class);
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
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        Map<String, Object> result = new HashMap<>();
        try {
            ResponseEntity<HashMap> response = restTemplate.exchange(HEALTH, HttpMethod.GET, requestEntity, HashMap.class);
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
        RestTemplate restTemplate = getRestTemplate();
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

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdRestWriter.class);

    public void setSimulateOnly(boolean simulateOnly) {
        this.simulateOnly = simulateOnly;
    }

    public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        logger.info("Processing [{}] cross references - simulate [{}]", referenceInputBeans.size(), simulateOnly);
        if (simulateOnly)
            return 0;
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<CrossReferenceInputBean>> requestEntity = new HttpEntity<>(referenceInputBeans, httpHeaders);
        try {
            ResponseEntity<ArrayList> response = restTemplate.exchange(CROSS_REFERENCES, HttpMethod.POST, requestEntity, ArrayList.class);
            logServerMessages(response);
            return referenceInputBeans.size();
        } catch (HttpClientErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return 0;
        } catch (HttpServerErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return 0;

        }

    }

    public Collection<Tag> getCountries() throws FlockException {
//        if (simulateOnly)
//            return null;
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<TagInputBean>> requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<String> response = restTemplate.exchange(COUNTRIES, HttpMethod.GET, requestEntity, String.class);
            TypeFactory typeFactory = mapper.getTypeFactory();
            CollectionType collectionType = typeFactory.constructCollectionType(ArrayList.class, TagInputBean.class);
            return mapper.readValue(response.getBody(), collectionType);
        } catch (HttpClientErrorException e) {
            // ToDo: Rest error handling pretty useless. need to know why it's failing
            logger.error("Client tracking error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("Client tracking error {}", getErrorMessage(e));
            return null;

        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
        return null;
    }

    public String flushEntities(Company company, List<EntityInputBean> entityInputs, boolean async) throws FlockException {
        if (simulateOnly || entityInputs.isEmpty())
            return "OK";
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<EntityInputBean>> requestEntity = new HttpEntity<>(entityInputs, httpHeaders);

        try {
            restTemplate.exchange( NEW_ENTITY+"?async="+async, HttpMethod.PUT, requestEntity, TrackResultBean.class);
            return "OK";
        } catch (HttpClientErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;

        }
    }

    RestTemplate restTemplate= null;
    private RestTemplate getRestTemplate() {
        if ( restTemplate == null ) {
            restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        }
        return restTemplate;
    }

    public String flushTags(List<TagInputBean> tagInputBean) throws FlockException {
        if (tagInputBean.isEmpty())
            return "OK";
        RestTemplate restTemplate = getRestTemplate();

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

    public void ensureFortress(String fortressName) throws FlockException {
        if (fortressName == null)
            return;

        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<FortressInputBean> request = new HttpEntity<>(new FortressInputBean(fortressName), httpHeaders);
        try {
            restTemplate.exchange(FORTRESS, HttpMethod.POST, request, FortressResultBean.class);
            if (defaultFortress != null && !defaultFortress.equals(fortressName)) {
                request = new HttpEntity<>(new FortressInputBean(defaultFortress, false), httpHeaders);
                restTemplate.exchange(FORTRESS, HttpMethod.POST, request, FortressResultBean.class);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // ToDo: Rest error handling pretty useless. need to know why it's failing
            logger.error("Service Tracking error {}", getErrorMessage(e));
        }
    }

    public String getErrorMessage(HttpStatusCodeException e) throws FlockException {

        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR || e.getStatusCode()== HttpStatus.BAD_REQUEST|| e.getStatusCode()== HttpStatus.BAD_REQUEST) {
            logger.error(e.getResponseBodyAsString());
            String error = e.getResponseBodyAsString();
            if ( error.contains("Invalid API"))
                logger.info("Your API key appears to be invalid. Have you run the configure process?");
            throw new FlockException(error);
        }

        JsonNode n = null;
        try {
            n = mapper.readTree(e.getResponseBodyAsByteArray());
        } catch (IOException e1) {

            logger.error(String.valueOf(e1));
        }
        String message;
        if (n != null) {
            message = String.valueOf(n.get("message"));
        }
        else
            message = e.getMessage();

        return message;
    }

    private static HttpHeaders httpHeaders = null;

    /**
     * Simple header with no authorisation
     * @return unauthenticated header
     */
    private static HttpHeaders getHeaders() {
        return new HttpHeaders() {
            {
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", CompressionHelper.charSet.toString());

                if (compress)
                    set("Accept-Encoding", "gzip,deflate");
            }
        };

    }



    public static HttpHeaders getHeaders(final String apiKey, final String userName, final String password) {
        if (httpHeaders != null)
            return httpHeaders;

        httpHeaders = new HttpHeaders() {
            {
                if (userName != null && password != null) {
                    String auth = userName + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(
                            auth.getBytes(Charset.forName("US-ASCII")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                }

                if ( apiKey != null )
                    set("Api-Key", apiKey);

                setContentType(MediaType.APPLICATION_JSON);
                set("charset", CompressionHelper.charSet.toString());

                if (compress)
                    set("Accept-Encoding", "gzip,deflate");
            }
        };

        return httpHeaders;
    }

    public static Map<String, Object> getWeightedMap(int weight) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("weight", weight);
        return properties;
    }


//    public void flush(String message) throws FlockException {
//        flush(message, ProfileConfiguration.DataType.TAG);
//        flush(message, ProfileConfiguration.DataType.TRACK);
//    }

    /**
     * push any remaining updates
     */
//    public void flush(String message, ProfileConfiguration.DataType dataType) throws FlockException {
//        if (simulateOnly)
//            return;
//        if (dataType.equals(ProfileConfiguration.DataType.TRACK)) {
//            synchronized (entitySync) {
//                track(null, true, message);
//
//            }
//        } else {
//            synchronized (tagSync) {
//                batch(null, true, message);
//            }
//        }
//    }



}
