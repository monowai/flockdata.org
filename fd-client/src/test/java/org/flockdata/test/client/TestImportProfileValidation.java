/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.client;

import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

/**
 * Parameter import checks for ImportProfile
 *
 * Created by mike on 28/01/15.
 */
public class TestImportProfileValidation extends AbstractImport{
    @Test
    public void valid_Properties() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/properties-rlx.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ClientConfiguration.getImportProfile("/properties-rlx.json");
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        params.setFortressName(null);
        try {
            fileProcessor.processFile(params, "/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No fortress name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("fortressName attribute."));
        }
        // Should also fail with blank
        params.setFortressName("");
        try {
            fileProcessor.processFile(params, "/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No fortress name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("fortressName attribute."));
        }

        params.setFortressName("abc");

        params.setDocumentName(null);
        try {
            fileProcessor.processFile(params, "/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No document name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("documentName attribute."));
        }

        params.setDocumentName("");
        try {
            fileProcessor.processFile(params, "/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No document name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("documentName attribute."));
        }

    }


}
