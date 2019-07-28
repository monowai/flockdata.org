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

import java.util.Collection;
import java.util.List;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;

/**
 * Functions to read and write data to the service. This may involve buffering data for a
 * batched dispatch. This is the preferred interface to injects into you code to handle
 * reading and writing of entities and tags
 * <p>
 * This class relies on an FdIoInterface to do the actual communications.
 *
 * @author mholdsworth
 * @tag Rest, Integration, Entity, Tag
 * @see org.flockdata.transform.FdIoInterface
 * @since 5/03/2016
 */
public interface Template {

  /**
   * @return access to the IO routines that talk to FlockData
   */
  FdIoInterface getFdIoInterface();

  /**
   * @param tagInputBean payload
   * @throws FlockException failure to communicate to the services
   */
  void writeTag(TagInputBean tagInputBean) throws FlockException;

  /**
   * @param tagInputBean collection of tags
   * @throws FlockException failure to communicate to the services
   */
  void writeTags(Collection<TagInputBean> tagInputBean) throws FlockException;

  /**
   * @param entityInputBean payload
   * @throws FlockException failure to communicate to the service
   */
  void writeEntity(EntityInputBean entityInputBean) throws FlockException;

  /**
   * @param entityInputBean payload
   * @param flush           force a flush of any cached data
   * @throws FlockException failure to communicate to the service
   */
  void writeEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException;

  /**
   * Push all payloads to the service when the number of objects to write hits the value
   * specified by org.fd.client.batchsize
   */
  void flush();

  /**
   * clear down any cached payloads
   */
  void reset();

  /**
   * @return cached entity payloads
   */
  List<EntityInputBean> getEntities();

  /**
   * @return cached tag payloads
   */
  List<TagInputBean> getTags();

  /**
   * Convenience function that calls out to a remote instance and validates that:
   * Client can see the Service
   * Authentication passes
   * APIKey, to use for track activities, is returned
   *
   * @return Implementation dependant - generally the logged-in-user but is NULL for server side and testing impls
   * @throws FlockException Validation errors
   */
  SystemUserResultBean validateConnectivity() throws FlockException;
}
