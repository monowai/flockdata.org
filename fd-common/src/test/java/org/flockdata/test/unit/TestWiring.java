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

package org.flockdata.test.unit;

import static junit.framework.TestCase.assertNotNull;

import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.FileProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mike
 * @tag
 * @since 17/12/16
 */
@ContextConfiguration(classes = {
    ClientConfiguration.class,
    AmqpRabbitConfig.class,
    Exchanges.class,
    FileProcessor.class

})
@RunWith(SpringRunner.class)
@ActiveProfiles( {"dev"})
public class TestWiring {

  private ClientConfiguration clientConfiguration;
  private AmqpRabbitConfig rabbitConfig;
  private FileProcessor fileProcessor;

  @Autowired
  void setServices(ClientConfiguration clientConfiguration,
                   AmqpRabbitConfig rabbitConfig,
                   FileProcessor fileProcessor) {
    this.clientConfiguration = clientConfiguration;
    this.rabbitConfig = rabbitConfig;
    this.fileProcessor = fileProcessor;
  }

  @Test
  public void autowiring() throws Exception {
    // Ensure basic components deploy with sensible defaults
    assertNotNull(clientConfiguration);
    assertNotNull(rabbitConfig);
    assertNotNull(fileProcessor);
  }
}
