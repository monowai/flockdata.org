/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 1/03/15.
 */
public class TestNestedTags extends AbstractImport {

    @Test
    public void label_missingColumnDoesNotCreateTargetTag() throws Exception {
        ClientConfiguration configuration= getClientConfiguration();
        FileProcessor fileProcessor = new FileProcessor();
        fileProcessor.processFile(ProfileReader.getImportProfile("/profile/interest-groups.json"),
                "/data/tags-inputs.csv", getFdWriter(), null, configuration);

        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        // The profile defines a nested tag but the value is missing in the source

        assertEquals(1, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            System.out.println(tagInputBean);
            assertFalse("The target tag should not exist as the source value was missing", tagInputBean.hasTargets());
        }
    }
}
