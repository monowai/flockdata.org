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
import org.flockdata.transform.FdWriter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import javax.annotation.PostConstruct;
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
@Profile("fd-batch")
public class FdRestWriter implements FdWriter {

    private String TRACK;
    private String NEW_TAG;
    private String FORTRESS;
    private String PING;
    private String ME;
    private String REGISTER;
    private String apiKey;
    private int batchSize;
    private static boolean compress = true;
    private boolean simulateOnly;
    private boolean validateOnly = false;
    private String defaultFortress;

    @Autowired
    ClientConfiguration configuration;

    private ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

    @Autowired
    private AmqpServices amqpServices ;

    private FdRestWriter(){}

    @PostConstruct
    void init() {
        httpHeaders = null;
        this.apiKey = configuration.getApiKey();
        this.validateOnly = configuration.isValidateOnly();
        // Urls to write Entity/Tag/Fortress information
        this.TRACK = configuration.getEngineURL() + "/v1/track/";
        this.PING = configuration.getEngineURL() + "/v1/ping/";
        this.REGISTER = configuration.getEngineURL() + "/v1/profiles/";
        this.ME = configuration.getEngineURL() + "/v1/profiles/me/";
        this.NEW_TAG = configuration.getEngineURL() + "/v1/tag/";
        this.FORTRESS = configuration.getEngineURL() + "/v1/fortress/";
        this.batchSize = configuration.getBatchSize();
        simulateOnly = batchSize < 1;

    }

    @Deprecated
    // Call with the configuration version
    public FdRestWriter(String serverName, String apiKey, int batchSize, String defaultFortress) {
        this();
        httpHeaders = null;
        this.apiKey = apiKey;
        // Urls to write Entity/Tag/Fortress information
        this.TRACK = serverName + "/v1/track/";
        this.PING = serverName + "/v1/ping/";
        this.REGISTER = serverName + "/v1/profiles/";
        this.ME = serverName + "/v1/profiles/me/";
        this.NEW_TAG = serverName + "/v1/tag/";
        this.FORTRESS = serverName + "/v1/fortress/";
        this.batchSize = batchSize;
        this.defaultFortress = defaultFortress;
        simulateOnly = batchSize < 1;
    }

    public SystemUserResultBean me() {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(apiKey);// Unauthorized ping is ok
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

    public boolean isSimulateOnly() {
        return simulateOnly;
    }

    public SystemUserResultBean registerProfile(String authUser, String authPass, String userName, String company) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHeaders(null); // Internal application authorisation
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

    private String flushEntitiesAmqp(Collection<EntityInputBean> entityInputs) throws FlockException {
        try {
            // DAT-373
            amqpServices.publish(entityInputs);
        } catch (IOException ioe) {
            logger.error(ioe.getLocalizedMessage());
            throw new FlockException("IO Exception", ioe.getCause());
        }
        return "OK";

    }

    public String flushEntities(Company company, List<EntityInputBean> entityInputs, ClientConfiguration configuration) throws FlockException {
        if (simulateOnly || entityInputs.isEmpty())
            return "OK";

        if (configuration.isValidateOnly()) {
            return validateOnly(entityInputs);

        }

        if (configuration.isAmqp())
            return flushEntitiesAmqp(entityInputs);
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = getHeaders(apiKey);
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

        HttpHeaders httpHeaders = getHeaders(apiKey);
        //HttpEntity<List<EntityInputBean>> requestEntity = new HttpEntity<>(entityInputs, httpHeaders);

        try {
            Map<String, Object> params = new HashMap<>();
            for (EntityInputBean entityInput : entityInputs) {
                params.put("fortress", entityInput.getFortressName());
                params.put("documentName", entityInput.getDocumentType().getName());
                params.put("code", entityInput.getCode());
                HttpEntity<EntityBean> found = restTemplate.exchange(TRACK + "/{fortress}/{documentName}/{code}", HttpMethod.GET, new HttpEntity<>(httpHeaders), EntityBean.class, params);

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

        HttpHeaders httpHeaders = getHeaders(apiKey);
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

        HttpHeaders httpHeaders = getHeaders(apiKey);
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

    private HttpHeaders httpHeaders = null;

    public HttpHeaders getHeaders(final String apiKey) {
        if (httpHeaders != null)
            return httpHeaders;

        httpHeaders = new HttpHeaders() {
            {
                if (configuration.getHttpUser() != null && configuration.getHttpPass() != null) {
                    String auth = configuration.getHttpUser() + ":" + configuration.getHttpPass();
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
                ", userName='" + configuration.getHttpUser() + '\'' +
                ", simulateOnly=" + simulateOnly +
                ", batchSize=" + batchSize +
                '}';
    }
}
