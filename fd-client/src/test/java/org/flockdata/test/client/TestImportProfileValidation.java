/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

/**
 * Parameter import checks for ImportProfile
 *
 * Created by mike on 28/01/15.
 */
public class TestImportProfileValidation extends AbstractImport{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void valid_Properties() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/properties-rlx.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setLoginUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/properties-rlx.json");
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        params.setFortressName(null);
        try {
            fileProcessor.processFile(params, "/data/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No fortress name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("fortressName attribute."));
        }
        // Should also fail with blank
        params.setFortressName("");
        try {
            fileProcessor.processFile(params, "/data/properties-rlx.txt", getFdWriter(), null, configuration);
            fail("No fortress name found. We should not have gotten here");
        } catch (FlockException e){
            assertTrue(e.getMessage().contains("fortressName attribute."));
        }

        params.setFortressName("abc");
        exception.expect(IllegalArgumentException.class);
        params.setDocumentName(null);
        try {
            fileProcessor.processFile(params, "/data/properties-rlx.txt", getFdWriter(), null, configuration);
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
