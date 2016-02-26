/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVTagsWithDelimiter extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String file = "/profile/no-header.json";
        ClientConfiguration configuration = getClientConfiguration();
        assertNotNull(configuration);
        configuration.setLoginUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile(file);
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/no-header.txt", getFdWriter(), null, configuration);
        int expectedRows = 6;
        assertEquals(expectedRows, rows);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        TestCase.assertEquals(expectedRows, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            assertFalse(tagInputBean.getCode().contains("|"));
            assertFalse(tagInputBean.getName().contains("|"));
            TestCase.assertEquals(1, tagInputBean.getTargets().size());
            assertFalse("non-persistent mapping was not ignored", tagInputBean.hasTagProperties());
            Collection<TagInputBean> targets = tagInputBean.getTargets().get("represents");
            for (TagInputBean represents : targets ) {
                assertFalse(represents.getCode().contains("|"));
                assertTrue(represents.isMustExist());

            }
        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(tagInputBeans);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }


}
