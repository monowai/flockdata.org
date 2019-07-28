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

package org.flockdata.test.unit.client;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FileProcessor;
import org.flockdata.integration.Template;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Simple ancestor for encapsulating profile and writer functionality
 *
 * @author mholdsworth
 * @since 12/02/2015
 */
@RunWith(SpringRunner.class)
@ActiveProfiles( {"dev"})
@ContextConfiguration(classes = {
    ClientConfiguration.class,
    FdTemplateMock.class,
    FileProcessor.class,
    FdMockIo.class,
})
@TestPropertySource( {"/application_dev.properties"})
public class AbstractImport {
  // Re-implement an FdWriter class if you want to validate data in the flush routines

  @Autowired
  protected Template fdTemplate;
  @Autowired
  protected FileProcessor fileProcessor;
  @Autowired
  protected ClientConfiguration clientConfiguration;

  protected Template getTemplate() {
    return fdTemplate;
  }

  /**
   * Clear out any cached data in the template
   * <p>
   * For testing purposes we need to analyse the batched payload without flushing
   * If we don't reset the batched payload then any previous runs contents will also be in the result
   */
  @Before
  public void clearLoader() {
    fdTemplate.reset();
    assertNotNull(fdTemplate.getFdIoInterface());
  }

  @Test
  public void autoWiringWorks() {
    assertNotNull(clientConfiguration);
    assertTrue("" + clientConfiguration.batchSize(), clientConfiguration.batchSize() > 10);
    assertNotNull(fdTemplate);
    assertNotNull(fileProcessor);
  }


}
