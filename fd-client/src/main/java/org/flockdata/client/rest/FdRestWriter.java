/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.model.Company;
import org.flockdata.registration.*;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLinkInputBean;
import org.flockdata.transform.FdLoader;
import org.flockdata.transform.FdWriter;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Template to support writing Entity and Tag information to a remote FlockData service
 *
 * @see org.flockdata.client.Importer
 * <p>
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
@Component
public class FdRestWriter implements FdWriter {

    private String TRACK;
    private String NEW_TAG;
    private String CROSS_REFERENCES;
    private String FORTRESS;
    private String PING;
    private String AUTH_PING;
    private String ME;
    private String HEALTH;
    private String REGISTER;
    private String userName;
    private String password;
    private String apiKey;
    private int batchSize;
    private static boolean compress = true;
    private boolean simulateOnly;
    private boolean validateOnly = false;
    private String defaultFortress;
    ClientConfiguration configuration;

    private ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    private AmqpServices amqpServices = null;

    FdRestWriter () {}

    /**
     * Use this version for administrative access where the username and password must exist
     *
     * @param serverName where are we talking to?
     * @param userName   configured user in the security domain
     * @param password   configured password in the security domain
     * @param batchSize  default batch command size
     */
    @Deprecated
    public FdRestWriter(String serverName, String userName, String password, int batchSize) {
        this(serverName, null, userName, password, batchSize, null);
    }

    public FdRestWriter(ClientConfiguration configuration) {
        httpHeaders = null;
        this.configuration = configuration;
        this.apiKey = configuration.getApiKey();
        this.validateOnly = configuration.isValidateOnly();
        // Urls to write Entity/Tag/Fortress information
        this.TRACK = configuration.getEngineURL() + "/v1/track/";
        this.AUTH_PING = configuration.getEngineURL() + "/v1/admin/ping/";
        this.PING = configuration.getEngineURL() + "/v1/ping/";
        this.REGISTER = configuration.getEngineURL() + "/v1/profiles/";
        this.ME = configuration.getEngineURL() + "/v1/profiles/me/";
        this.HEALTH = configuration.getEngineURL() + "/v1/admin/health/";
        this.CROSS_REFERENCES = configuration.getEngineURL() + "/v1/track/link/";
        this.NEW_TAG = configuration.getEngineURL() + "/v1/tag/";
        this.FORTRESS = configuration.getEngineURL() + "/v1/fortress/";
        this.batchSize = configuration.getBatchSize();
        simulateOnly = batchSize < 1;

    }

    @Deprecated
    // Call with the configuration version
    public FdRestWriter(String serverName, String apiKey, String userName, String password, int batchSize, String defaultFortress) {
        httpHeaders = null;
        this.userName = userName;
        this.password = password;
        this.apiKey = apiKey;
        // Urls to write Entity/Tag/Fortress information
        this.TRACK = serverName + "/v1/track/";
        this.AUTH_PING = serverName + "/v1/admin/ping/";
        this.PING = serverName + "/v1/ping/";
        this.REGISTER = serverName + "/v1/profiles/";
        this.ME = serverName + "/v1/profiles/me/";
        this.HEALTH = serverName + "/v1/admin/health/";
        this.CROSS_REFERENCES = serverName + "/v1/track/xref/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
        simulateOnly = batchSize < 1;
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
    public static Map<String, Object> convertToMap(Object o) {
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
        return om.convertValue(o, Map.class);
    }

    public SystemUserResultBean me() {
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
     * @return "pong"
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
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
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

    public int flushEntityLinks(List<EntityLinkInputBean> referenceInputBeans) throws FlockException {
        logger.info("Processing [{}] cross references - simulate [{}]", referenceInputBeans.size(), simulateOnly);
        if (simulateOnly)
            return 0;
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<EntityLinkInputBean>> requestEntity = new HttpEntity<>(referenceInputBeans, httpHeaders);
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

    @Override
    public void close(FdLoader fdLoader) throws FlockException {
        fdLoader.flush();
        if (amqpServices != null) {
            amqpServices.close();
            amqpServices = null;
        }
    }

    public String flushEntitiesAmqp(Collection<EntityInputBean> entityInputs, ClientConfiguration configuration) throws FlockException {
        try {
            // DAT-373
            getAmqpHelper(configuration).publish(entityInputs);
        } catch (IOException ioe) {
            logger.error(ioe.getLocalizedMessage());
            throw new FlockException("IO Exception", ioe.getCause());
        }
        return "OK";

    }

    private AmqpServices getAmqpHelper(ClientConfiguration configuration) throws FlockException {
        if (amqpServices == null)
            amqpServices = new AmqpServices(configuration);
        return amqpServices;
    }

    public String flushEntities(Company company, List<EntityInputBean> entityInputs, ClientConfiguration configuration) throws FlockException {
        if (simulateOnly || entityInputs.isEmpty())
            return "OK";

        if (configuration.isValidateOnly()) {
            return validateOnly(entityInputs);

        }

        if (configuration.isAmqp())
            return flushEntitiesAmqp(entityInputs, configuration);
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<EntityInputBean>> requestEntity = new HttpEntity<>(entityInputs, httpHeaders);

        try {
            restTemplate.exchange(TRACK + "?async=" + configuration.isAsync(), HttpMethod.PUT, requestEntity, Void.class);
            return "OK";
        } catch (HttpClientErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;

        }
    }

    private String validateOnly(List<EntityInputBean> entityInputs) throws FlockException {
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        //HttpEntity<List<EntityInputBean>> requestEntity = new HttpEntity<>(entityInputs, httpHeaders);

        try {
            Map<String, Object> params = new HashMap<>();
            for (EntityInputBean entityInput : entityInputs) {
                params.put("fortress", entityInput.getFortressName());
                params.put("documentName", entityInput.getDocumentType().getName());
                params.put("code", entityInput.getCode());
                HttpEntity<EntityBean> found = restTemplate.exchange(TRACK + "/{fortress}/{documentName}/{code}", HttpMethod.GET, new HttpEntity<Object>(httpHeaders), EntityBean.class, params);

                //Object object = restTemplate.getForObject(TRACK + "{fortress}/{documentType}/{callerRef}", EntityBean.class, params);
                //HttpEntity<EntityBean> found = restTemplate.getForEntity(TRACK, EntityBean.class, params );
                if (found == null || found.getBody() == null) {
                    logger.info("Not Found {}", entityInput);
                }
            }

            //restTemplate.exchange(TRACK , HttpMethod.GET, requestEntity, EntityBean.class);
            return "OK";
        } catch (HttpClientErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("Service tracking error {}", getErrorMessage(e));
            return null;

        }

    }

    RestTemplate restTemplate = null;

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        }
        return restTemplate;
    }

    public String flushTags(List<TagInputBean> tagInputs) throws FlockException {
        if (tagInputs.isEmpty())
            return "OK";
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey, userName, password);
        HttpEntity<List<TagInputBean>> requestEntity = new HttpEntity<>(tagInputs, httpHeaders);

        try {
            // ToDo logServerMessage - error state will be returned in arraylist
            ResponseEntity<ArrayList> response = restTemplate.exchange(NEW_TAG, HttpMethod.PUT, requestEntity, ArrayList.class);
            logServerMessages(response);
            return "OK";
        } catch (HttpClientErrorException e) {
            // to test, try to log against no existing fortress.
            logger.error("FlockData client error processing Tags {}", getErrorMessage(e));
            return null;
        } catch (HttpServerErrorException e) {
            logger.error("FlockData server error processing Tags {}", getErrorMessage(e));
            return null;
        } catch (ResourceAccessException e) {
            logger.error("Unable to talk to FD over the REST interface. Can't process this tag request. " + (configuration != null && configuration.isAmqp()?"This does not affect sending payloads over AMQP":""));
            return null;
        }
    }

    private void logServerMessages(ResponseEntity<ArrayList> response) {
        ArrayList x = response.getBody();
        if (x != null)
            for (Object val : x) {
                Map map = (Map) val;
                if (map.containsKey("serviceMessage")) {
                    Object serviceMessage = map.get("serviceMessage");
                    if (serviceMessage != null)
                        logger.error("Service returned [{}]", serviceMessage.toString());
                }
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

        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR || e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
            logger.error(e.getResponseBodyAsString());
            String error = e.getResponseBodyAsString();
            if (error.contains("Invalid API"))
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
        } else
            message = e.getMessage();

        return message;
    }

    private static HttpHeaders httpHeaders = null;

    /**
     * Simple header with no authorisation
     *
     * @return unauthenticated header
     */
    private static HttpHeaders getHeaders() {
        return new HttpHeaders() {
            {
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", ObjectHelper.charSet.toString());

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
                            auth.getBytes(Charset.forName("UTF-8")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                }

                if (apiKey != null)
                    set("api-key", apiKey);

                setContentType(MediaType.APPLICATION_JSON);
                set("charset", ObjectHelper.charSet.toString());

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

    @Override
    public String toString() {
        return "FdRestWriter{" +
                "PING='" + PING + '\'' +
                ", userName='" + userName + '\'' +
                ", simulateOnly=" + simulateOnly +
                ", batchSize=" + batchSize +
                '}';
    }
}
