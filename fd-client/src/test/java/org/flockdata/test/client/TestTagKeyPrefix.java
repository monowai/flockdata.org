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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by mike on 22/07/15.
 */
public class TestTagKeyPrefix extends AbstractImport {
    private Logger logger = LoggerFactory.getLogger(TestTagKeyPrefix.class);
    @Test
    public void prefix_TagKeyWorks() throws Exception {
        ClientConfiguration configuration= getClientConfiguration();
        FileProcessor fileProcessor = new FileProcessor();
        fileProcessor.processFile(ProfileReader.getImportProfile("/profile/tag-key-prefix.json"),
                "/data/tag-key-prefix.csv", getFdWriter(), null, configuration);

        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        // The profile defines a nested tag but the value is missing in the source

        assertEquals(2, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            switch (tagInputBean.getKeyPrefix()){
                case "UK":
                    validateCountryAndLiteralKeyPrefix(tagInputBean.getTargets().get("region"));
                    break;
                case "NZ":
                    validateCountryAndLiteralKeyPrefix(tagInputBean.getTargets().get("region"));
                    break;
                default:
                    throw new RuntimeException("Unexpected tag " +tagInputBean.toString());
            }
            logger.info(tagInputBean.toString());
        }
    }

    private void validateCountryAndLiteralKeyPrefix(Collection<TagInputBean> countries) {
        assertFalse(countries.isEmpty());
        TagInputBean country = countries.iterator().next();
        assertEquals("literal", country.getKeyPrefix());
    }
}
