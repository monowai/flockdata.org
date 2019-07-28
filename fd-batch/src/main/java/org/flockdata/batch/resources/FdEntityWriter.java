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

package org.flockdata.batch.resources;

import java.util.List;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.Template;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Batches the item to the payload ready for transmission
 *
 * @author nabil
 * @tag FdClient, Batch, Entity
 */
@Component
@Profile( {"fd-batch", "fd-batch-dev"})
@Service
public class FdEntityWriter implements ItemWriter<EntityInputBean> {


  private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdEntityWriter.class);
  private Template fdTemplate;

  private FdEntityWriter() {
  }

  @Autowired
  FdEntityWriter(Template fdTemplate) {
    this();
    this.fdTemplate = fdTemplate;
    try {
      fdTemplate.validateConnectivity();
    } catch (FlockException e) {
      logger.error("Error validating connectivity");
    }
  }

  @Override
  public void write(List<? extends EntityInputBean> items) throws Exception {
    for (EntityInputBean item : items) {
      fdTemplate.writeEntity(item);
    }
  }
}
