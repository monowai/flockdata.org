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

package org.flockdata.test.unit.client;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportContentModel;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.transform.ProfileReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

/**
 * Parameter import checks for ImportProfile
 * <p>
 * Created by mike on 28/01/15.
 */
public class TestImportProfileValidation extends AbstractImport {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void valid_Properties() throws Exception {

        ImportContentModel profile = ProfileReader.getContentModel("/model/properties-rlx.json");
        assertEquals(',', profile.getDelimiter());
        assertEquals(false, profile.hasHeader());
        profile.setFortress(null);
        exception.expect(FlockException.class);
        fileProcessor.processFile(profile, "/data/properties-rlx.txt");

        // Should also fail with blank
        profile.setFortress(new FortressInputBean(""));
        exception.expect(IllegalArgumentException.class);
        fileProcessor.processFile(profile, "/data/properties-rlx.txt");

        profile.setFortress(new FortressInputBean("Override The Name"));
        assertTrue("Blank fortressName was not overridden", profile.getFortress().getName().equals("Override The Name"));

        profile.setFortress(new FortressInputBean("abc"));
        assertFalse("Setting fortressName should not override a fortressObject", profile.getFortress().getName().equals("abc"));
        profile.setFortress(new FortressInputBean("Override The Name"));
        assertTrue(profile.getFortress().getName().equals("Override The Name"));

        exception.expect(IllegalArgumentException.class);
        profile.setDocumentName(null);
        try {
            fileProcessor.processFile(profile, "/data/properties-rlx.txt");
            fail("No document name found. We should not have gotten here");
        } catch (FlockException e) {
            assertTrue(e.getMessage().contains("documentName attribute."));
        }

        profile.setDocumentName("");
        try {
            fileProcessor.processFile(profile, "/properties-rlx.txt");
            fail("No document name found. We should not have gotten here");
        } catch (FlockException e) {
            assertTrue(e.getMessage().contains("documentName attribute."));
        }

    }


}
