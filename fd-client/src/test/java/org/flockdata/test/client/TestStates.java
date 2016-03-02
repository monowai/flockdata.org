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

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * General validation of states. Uses the resources from main/resources
 *
 * Created by mike on 9/03/15.
 */
public class TestStates extends AbstractImport {
    @Test
    public void validate_States() throws Exception {
        String profile = "/states.json";
        ClientConfiguration configuration = getClientConfiguration();
        FileProcessor fileProcessor = new FileProcessor();
        ContentProfileImpl params = ProfileReader.getImportProfile(profile);
        fileProcessor.processFile(params, "/states.csv", getFdWriter(), null, configuration);
        assertEquals(72, getFdWriter().getTags().size());

        for (TagInputBean stateTag : getFdWriter().getTags()) {
            assertEquals(1, stateTag.getTargets().size());
            TagInputBean country = stateTag.getTargets().get("region").iterator().next();
            assertNotNull(country);
            assertTrue(stateTag.getKeyPrefix().equals(country.getCode()));
            if (country.getCode().equals("US")){
                // Randomly check a US state for Census aliases
                if ( stateTag.getCode().equals("CA")){
                    assertNotNull ( stateTag.getAliases());
                    assertEquals(2, stateTag.getAliases().size());
                    for (AliasInputBean aliasInputBean : stateTag.getAliases()) {
                        if (aliasInputBean.getDescription().equals("USCensus"))
                            assertEquals(2, aliasInputBean.getCode().length());
                    }
                }
            } else {
                // Validate that iso+name exists
                assertNotNull ( stateTag.getAliases());
                assertEquals(1, stateTag.getAliases().size());
                assertEquals(stateTag.getName(), stateTag.getAliases().iterator().next().getCode());

            }
        }
    }
}
