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

package org.flockdata.transform;

import java.io.IOException;
import java.util.Collection;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.model.ExtractProfile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * Interface that deals with the physical communication between client and server
 * <p>
 * The
 *
 * @author mholdsworth
 * @tag Integration, FdClient, Contract
 * @since 7/10/2014
 */
public interface FdIoInterface {
  /**
   * @return currently logged in user
   */
  SystemUserResultBean me();

  /**
   * @param tagInputBeans tags to send to the service
   * @return error messages. "OK" if all is good
   * @throws FlockException communication exception resulting in a permanent failure
   */
  String writeTags(Collection<TagInputBean> tagInputBeans) throws FlockException;

  /**
   * @param entityBatch Entities to send to the service
   * @return error messages. "OK" if all is good
   * @throws FlockException communication exception resulting in a permanent failure
   */
  String writeEntities(Collection<EntityInputBean> entityBatch) throws FlockException;

  ContentModel getContentModel(Fortress fortress, Document documentType);

  ContentModel getContentModel(String modelKey) throws IOException;

  /**
   * used to validate connectivity to the service
   *
   * @return Currently logged in user
   * @throws FlockException connectivity exception occurs
   */
  SystemUserResultBean validateConnectivity() throws FlockException;

  SystemUserResultBean login(String userName, String password);

  /**
   * @return URL for service
   */
  String getUrl();

  RestTemplate getRestTemplate();

  HttpHeaders getHeaders();

  ExtractProfile getExtractProfile(String fileModel, ContentModel contentModel);


}
