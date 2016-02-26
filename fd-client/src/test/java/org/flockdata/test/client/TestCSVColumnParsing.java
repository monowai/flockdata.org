/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.client.Configure;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVColumnParsing extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String file = "/profile/column-parsing.json";
        ClientConfiguration configuration = getClientConfiguration();
        assertNotNull(configuration);

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/column-parsing.json");
        assertEquals(false, params.hasHeader());

        long rows = fileProcessor.processFile(params, "/data/pac.txt", getFdWriter(), null, configuration);
        assertEquals(1l, rows);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertNotNull ( tagInputBeans);
        assertEquals(4, tagInputBeans.size());
        boolean foundA = false, foundB= false,foundC= false, foundD= false;
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("OSCategory")){
                foundA = true;
                assertEquals("E1140", tagInputBean.getCode());
            } else if ( tagInputBean.getLabel().equals("Expenditure")){
                foundB = true;
                assertEquals("D", tagInputBean.getCode());
                assertEquals("Direct", tagInputBean.getName());
            }  else if ( tagInputBean.getLabel().equals("InterestGroup")){
                foundC = true;
                assertEquals("C00485250", tagInputBean.getCode());
            }  else if ( tagInputBean.getLabel().equals("Politician")){
                foundD = true;
                assertEquals("N00031647", tagInputBean.getCode());
            }

        }
        assertTrue("Failed to find OS Category Tag", foundA);
        assertTrue("Failed to find Expenditure Tag", foundB);
        assertTrue("Failed to find InterestGroup Tag", foundC);
        assertTrue("Failed to find Politician Tag", foundD);
        for (EntityInputBean entityInputBean : getFdWriter().getEntities()) {
            assertEquals("4111320141231324700", entityInputBean.getCode());
        }
    }

    @Test
    public void segment_SetInPayloadFromSource() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/column-parsing.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setLoginUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/column-parsing.json");
        assertEquals(false, params.hasHeader());

        long rows = fileProcessor.processFile(params, "/data/pac.txt", getFdWriter(), null, configuration);
        assertEquals(1l, rows);
        for (EntityInputBean entityInputBean : getFdWriter().getEntities()) {
            assertEquals("The segment was not set in to the EntityInput", "2014", entityInputBean.getSegment());
        }
    }



}
