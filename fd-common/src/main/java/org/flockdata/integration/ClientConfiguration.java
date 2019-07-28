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

package org.flockdata.integration;

import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.experimental.Accessors;
import org.flockdata.registration.SystemUserResultBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configures fd-client from properties source from a configuration file
 *
 * @author mholdsworth
 * @tag Configuration, FdClient
 * @since 30/04/2014
 */
@Configuration
@Data
@Accessors(fluent = true)
public class ClientConfiguration {

  public static final String AMQP = "org.fd.client.amqp";
  public static final String KEY_MSG_KEY = "fd-apiKey";
  public static final String KEY_MSG_TYPE = "fd-type";
  private static final String KEY_ENGINE_API = "org.fd.engine.api";
  private static final String KEY_COMPANY = "org.fd.client.default.company";
  private static final String KEY_FORTRESS = "org.fd.client.default.fortress";
  private static final String KEY_API_KEY = "org.fd.client.apikey";
  private static final String KEY_HTTP_USER = "org.fd.client.http.user";
  private static final String KEY_HTTP_PASS = "org.fd.client.http.pass";
  private static final String KEY_BATCH_SIZE = "org.fd.client.batchsize";
  private static final String KEY_TRACK_QUEUE = "org.fd.track.messaging.queue";
  private static final String KEY_TRACK_EXCHANGE = "org.fd.messaging.exchange";
  private static final String KEY_TRACK_BINDING = "org.fd.track.messaging.binding";

  @Value("${" + KEY_COMPANY + ":flockdata}")
  private String company;

  @Value("${" + KEY_FORTRESS + ":#{null}}")
  private String fortress;

  @Value("${" + KEY_TRACK_QUEUE + ":fd.track.queue}")
  private String trackQueue;

  @Value("${" + KEY_TRACK_EXCHANGE + ":fd}")
  private String fdExchange;

  @Value("${" + KEY_TRACK_BINDING + ":fd.track.queue}")
  private String trackRoutingKey;

  @Value("${" + KEY_ENGINE_API + ":http://localhost:14001}")
  private String engineUrl;

  @Value("${" + KEY_API_KEY + ":#{null}}")
  private String apiKey;

  @Value("${" + KEY_HTTP_USER + ":demo}")
  private String httpUser;

  @Value("${" + KEY_HTTP_PASS + ":123}")
  private String httpPass;

  @Value("${" + KEY_BATCH_SIZE + ":1}")
  private int batchSize;

  private boolean amqp = true;

  private int skipCount = 0;

//  @Bean
//  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
//    return new PropertySourcesPlaceholderConfigurer();
//  }

  @Override
  @PostConstruct
  public String toString() {
    String auth;
    if (apiKey != null && !apiKey.isEmpty()) {
      auth = KEY_API_KEY + ": \"** set **\"";
    } else {
      auth = KEY_HTTP_USER + ": \"" + httpUser + "\"";
    }

    return "{" +
        "" + KEY_ENGINE_API + ": \"" + engineUrl + "\", " + auth + " }";
  }

  public String getServiceUrl() {
    if (engineUrl != null && !engineUrl.equals("") && !engineUrl.startsWith("http")) {
      engineUrl = "http://" + engineUrl;
    }
    return engineUrl;
  }

  public boolean isApiKeyValid() {
    return !(apiKey == null || apiKey.isEmpty());
  }

  public void setSystemUser(SystemUserResultBean systemUser) {
    if (systemUser != null) {
      apiKey(systemUser.getApiKey());
      httpUser(systemUser.getLogin());
      company(systemUser.getCompanyName());
    }
  }
}
