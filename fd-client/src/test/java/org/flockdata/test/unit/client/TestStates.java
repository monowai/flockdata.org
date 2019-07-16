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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * General validation of states
 *
 * @author mholdsworth
 * @since 9/03/2015
 */
public class TestStates extends AbstractImport {
    @Test
    public void validate_States() throws Exception {
        String file = "/states.json";
        ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
        fileProcessor.processFile(new ExtractProfileHandler(contentModel), "/states.csv");
        assertEquals(72, getTemplate().getTags().size());

        for (TagInputBean stateTag : getTemplate().getTags()) {
            assertEquals(1, stateTag.getTargets().size());
            TagInputBean country = stateTag.getTargets().get("region").iterator().next();
            assertNotNull(country);
            TestCase.assertEquals(stateTag.getKeyPrefix(), country.getCode());
            if (country.getCode().equals("US")) {
                // Randomly check a US state for Census aliases
                if (stateTag.getCode().equals("CA")) {
                    assertNotNull(stateTag.getAliases());
                    assertEquals(2, stateTag.getAliases().size());
                    for (AliasInputBean aliasInputBean : stateTag.getAliases()) {
                        if (aliasInputBean.getDescription().equals("USCensus")) {
                            assertEquals(2, aliasInputBean.getCode().length());
                        }
                    }
                }
            } else {
                // Validate that iso+name exists
                assertNotNull(stateTag.getAliases());
                assertEquals(1, stateTag.getAliases().size());
                assertEquals(stateTag.getName(), stateTag.getAliases().iterator().next().getCode());

            }
        }
    }
}
