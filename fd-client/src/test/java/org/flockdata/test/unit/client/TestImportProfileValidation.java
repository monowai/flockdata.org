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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Parameter import checks for ImportProfile
 *
 * @author mholdsworth
 * @since 28/01/2015
 */
public class TestImportProfileValidation extends AbstractImport {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void valid_Properties() throws Exception {

    ContentModel profile = ContentModelDeserializer.getContentModel("/model/properties-rlx.json");
    ExtractProfile extractProfile = new ExtractProfileHandler(profile, false);
    extractProfile.setQuoteCharacter("|");
    assertEquals(',', extractProfile.getDelimiter());
    assertEquals(false, extractProfile.hasHeader());
    profile.setFortress(null);
    exception.expect(FlockException.class);
    fileProcessor.processFile(extractProfile, "/data/properties-rlx.txt");

    // Should also fail with blank
    profile.setFortress(new FortressInputBean(""));
    exception.expect(IllegalArgumentException.class);
    fileProcessor.processFile(extractProfile, "/data/properties-rlx.txt");

    profile.setFortress(new FortressInputBean("Override The Name"));
    assertTrue("Blank fortressName was not overridden", profile.getFortress().getName().equals("Override The Name"));

    profile.setFortress(new FortressInputBean("abc"));
    assertFalse("Setting fortressName should not override a fortressObject", profile.getFortress().getName().equals("abc"));
    profile.setFortress(new FortressInputBean("Override The Name"));
    assertTrue(profile.getFortress().getName().equals("Override The Name"));

    exception.expect(IllegalArgumentException.class);
    profile.setDocumentName(null);
    try {
      fileProcessor.processFile(extractProfile, "/data/properties-rlx.txt");
      fail("No document name found. We should not have gotten here");
    } catch (FlockException e) {
      assertTrue(e.getMessage().contains("documentName attribute."));
    }

    profile.setDocumentName("");
    try {
      fileProcessor.processFile(extractProfile, "/properties-rlx.txt");
      fail("No document name found. We should not have gotten here");
    } catch (FlockException e) {
      assertTrue(e.getMessage().contains("documentName attribute."));
    }

  }

  @Test
  public void argumentsAreTrimmed() throws Exception {

    ContentModel profile = ContentModelDeserializer.getContentModel(" /model/properties-rlx.json ");
    assertNotNull("Locating profile did not trim leading and trailing white space", profile);
    ExtractProfile extractProfile = new ExtractProfileHandler(profile, false);
    extractProfile.setQuoteCharacter("|");
    assertEquals(',', extractProfile.getDelimiter());
    assertEquals(false, extractProfile.hasHeader());
    profile.setFortress(null);
    exception.expect(FlockException.class);
    fileProcessor.processFile(extractProfile, " /data/properties-rlx.txt ");

  }


}
