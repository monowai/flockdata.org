/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestTagAliases extends AbstractImport {
    @Test
    public void string_csvTagAliases() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String paramFile = "/profile/csv-tag-alias.json";
        ClientConfiguration configuration = getClientConfiguration();

        ContentProfileImpl params = ProfileReader.getImportProfile(paramFile);
        fileProcessor.processFile(params, "/data/csv-tag-alias.txt", getFdWriter(), null, configuration);

        Collection<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(3, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            switch (tagInputBean.getCode()) {
                case "AL":
                    assertTrue(tagInputBean.hasAliases());
                    assertNotNull(tagInputBean.getNotFoundCode());
                    assertEquals(1, tagInputBean.getAliases().size());
                    assertEquals("1", tagInputBean.getAliases().iterator().next().getCode());
                    assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                    break;
                case "AK":
                    assertTrue(tagInputBean.hasAliases());
                    assertNotNull(tagInputBean.getNotFoundCode());
                    assertEquals(1, tagInputBean.getAliases().size());
                    assertEquals("2", tagInputBean.getAliases().iterator().next().getCode());
                    assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
                    break;
                case "AB":
                    assertFalse(tagInputBean.hasAliases());
                    break;
            }
        }
    }



}
