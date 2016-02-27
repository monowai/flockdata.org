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
