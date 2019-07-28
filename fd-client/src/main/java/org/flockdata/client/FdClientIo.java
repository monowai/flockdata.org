/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.client;

import com.rabbitmq.client.AlreadyClosedException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.codec.binary.Base64;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.CommandResponse;
import org.flockdata.client.commands.Login;
import org.flockdata.client.commands.ModelGet;
import org.flockdata.client.commands.SearchEsPost;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.QueryParams;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Dispatches Entity and Tag data requests to a FlockData service
 * using RabbitMQ + Http
 *
 * @author mholdsworth
 * @tag Messaging, FdClient
 * @see ClientConfiguration
 * @see FdRabbitClient
 * @since 13/10/2013
 */
@Service
@Profile("!fd-server")
public class FdClientIo implements FdIoInterface {

  private static boolean compress = true;
  private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdClientIo.class);
  private ClientConfiguration clientConfiguration;
  private FdRabbitClient fdRabbitClient;
  private RestTemplate restTemplate = null;
  private HttpHeaders httpHeaders = null;
  private Login login;
  private SearchEsPost postQuery;
  private ModelGet modelGet;


  @Autowired
  FdClientIo(ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  /**
   * Adds a user defined property called "weight"
   *
   * @param weight value to assign
   * @return current user defined properties (function chaining)
   */
  @SuppressWarnings("unused")
  public static Map<String, Object> getWeightedMap(int weight) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("weight", weight);
    return properties;
  }

  @Autowired
  void setCommands(SearchEsPost postQuery, ModelGet modelGet, Login login) {
    this.postQuery = postQuery;
    this.modelGet = modelGet;
    this.login = login;
  }

  @Autowired(required = false)
  void setFdRabbitClient(FdRabbitClient fdRabbitClient) {
    this.fdRabbitClient = fdRabbitClient;
  }

  public SystemUserResultBean me() {
    return login.exec(clientConfiguration.httpUser(),
        clientConfiguration.httpPass())
        .getResult();
  }

  private ClientConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  private String writeEntitiesAmqp(Collection<EntityInputBean> entityInputs) throws FlockException {
    try {
      if (clientConfiguration.apiKey() == null) {
        login();
      }
      fdRabbitClient.publish(entityInputs);
    } catch (IOException ioe) {
      logger.error(ioe.getLocalizedMessage());
      throw new FlockException("IO Exception", ioe.getCause());
    }
    return "OK";

  }

  private String writeTagsAmqp(Collection<TagInputBean> tagInputs) throws FlockException {
    try {
      if (clientConfiguration.apiKey() == null) {
        login();
      }
      fdRabbitClient.publishTags(tagInputs);
    } catch (IOException | AlreadyClosedException ioe) {
      logger.error(ioe.getLocalizedMessage());
      throw new FlockException("IO Exception", ioe.getCause());
    }
    return "OK";

  }

  public String writeEntities(Collection<EntityInputBean> entityInputs) throws FlockException {
    if (entityInputs.isEmpty()) {
      return "OK";
    }

    return writeEntitiesAmqp(entityInputs);

  }

  public RestTemplate getRestTemplate() {
    if (restTemplate == null) {
      setRestTemplate(new RestTemplate());
    }
    return restTemplate;
  }

  @Autowired(required = false)
  void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
  }

  public String writeTags(Collection<TagInputBean> tagInputs) throws FlockException {
    if (tagInputs.isEmpty()) {
      return "OK";
    }

    return writeTagsAmqp(tagInputs);

  }

  public HttpHeaders getHeaders() {
    return getHeaders(clientConfiguration.httpUser(),
        clientConfiguration.httpPass(),
        clientConfiguration.apiKey());
  }

  public HttpHeaders getHeaders(String user, String pass, final String apiKey) {
    String auth = user + ":" + pass;
    byte[] encodedAuth = Base64.encodeBase64(
        auth.getBytes(StandardCharsets.UTF_8));
    String authHeader = "Basic " + new String(encodedAuth);

    if (httpHeaders != null &&
        httpHeaders.get("Authorization") != null &&
        Objects.requireNonNull(httpHeaders
            .get("Authorization"))
            .iterator()
            .next()
            .equals(authHeader)) {
      return httpHeaders;
    }

    httpHeaders = new HttpHeaders() {
      {
        if (clientConfiguration.httpUser() != null &&
            clientConfiguration.httpPass() != null) {
          set("Authorization", authHeader);
        }

        if (apiKey != null && !apiKey.equals("")) {
          set("api-key", apiKey);
        }

        setContentType(MediaType.APPLICATION_JSON);
        set("charset", ObjectHelper.charSet.toString());

        if (compress) {
          set("Accept-Encoding", "gzip,deflate");
        }
      }
    };

    return httpHeaders;
  }

  @Override
  public String toString() {
    return "FdRestWriter{" +
        "userName='" + clientConfiguration.httpUser() + '\'' +
        ", serviceEndpoint='" + clientConfiguration.getServiceUrl() +
        '}';
  }

  public SystemUserResultBean login() {
    CommandResponse<SystemUserResultBean> response = login.exec(clientConfiguration.httpUser(), clientConfiguration.httpPass());
    if (response.getError() != null) {
      logger.error("Error logging in as [{}] - {}", getUser(), response.getError());
      return null;
    }

    SystemUserResultBean suResult = response.getResult();
    if (suResult != null) {
      if (suResult.getApiKey() != null) {
        logger.debug("Configuring apiKey for [{}] to [{}]", clientConfiguration.httpUser(), clientConfiguration.getServiceUrl());
        clientConfiguration.setSystemUser(suResult);
      } else {
        logger.debug("User [{}] authenticated at [{}] but is not a registered data access user", clientConfiguration.httpUser(), clientConfiguration.getServiceUrl());
      }

    }
    return suResult;

  }

  public ContentModel getContentModel(String modelKey) throws IOException {
    ContentModel contentModel;
    contentModel = ContentModelDeserializer.getContentModel(modelKey);
    if (contentModel == null) {
      // See if it can be found on the server
      // format is {fortress}:{docType}, or tag:doctype
      String[] args = modelKey.split(":");
      if (args.length == 2) {
        contentModel = getContentModel(args[0], args[1]);
      }
    }
    return contentModel;
  }

  public ContentModel getContentModel(String type, String clazz) {
    CommandResponse<ContentModel> response = modelGet.exec(type, clazz);
    String error = response.getError();
    if (error != null) {
      logger.error("Get Model resulted in {} for {} {} on {} for {}",
          error, type, clazz,
          clientConfiguration.getServiceUrl(),
          clientConfiguration.httpUser());
    }

    return response.getResult();
  }

  public ExtractProfile getExtractProfile(String name, ContentModel contentModel) {
    ExtractProfile extractProfile = null;

    try {
      extractProfile = ExtractProfileDeserializer.getImportProfile(name, contentModel);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);

    }
    if (extractProfile == null) {
      extractProfile = new ExtractProfileHandler(contentModel);
    }
    return extractProfile;
  }

  @Override
  public ContentModel getContentModel(Fortress fortress, Document documentType) {
    return getContentModel(fortress.getName(), documentType.getCode());
  }

  /**
   * Only called for integration testing - resets the host port which is proxied
   *
   * @param rabbitHost url
   * @param rabbitPort port
   */
  public void resetRabbitClient(String rabbitHost, Integer rabbitPort) {
    fdRabbitClient.resetRabbitClient(rabbitHost, rabbitPort);
  }

  @Override
  public SystemUserResultBean validateConnectivity() throws FlockException {
    boolean error = false;
    SystemUserResultBean me = null;
    if (clientConfiguration.apiKey() == null || clientConfiguration.apiKey().length() == 0) {
      error = true;
      me = me();
      if (me != null && me.isActive()) {   // Resolve the api key from a login result
        clientConfiguration.setSystemUser(me);
        error = false;
      }

    }
    if (error) {
      throw new FlockException(String.format("Connectivity failed to %s for user %s - apiKey set == [%b] Without a key track requests will fail", clientConfiguration.getServiceUrl(), clientConfiguration.httpUser(), clientConfiguration.isApiKeyValid()));
    }
    return me;
  }

  public SystemUserResultBean login(String user, String pass) {
    httpHeaders = null;
    getClientConfiguration()
        .httpUser(user)
        .httpPass(pass);

    return login();
  }


  public Map<String, Object> search(QueryParams qp) {
    CommandResponse<Map<String, Object>> response = postQuery.exec(qp);
    if (response.getError() != null) {
      logger.error(response.getError());
    }
    return response.getResult();
  }

  public String getUrl() {
    return clientConfiguration.getServiceUrl();
  }

  public String getUser() {
    return clientConfiguration.httpUser();
  }

  public String getPass() {
    return clientConfiguration.httpPass();
  }

  /**
   * Overrides the endpoint for service communications for integration testing purposes only.
   *
   * @param serviceUrl URL to set
   */
  public void setServiceUrl(String serviceUrl) {
    logger.debug("setting service URL to {}", serviceUrl);
    clientConfiguration.engineUrl(serviceUrl);
  }

}
