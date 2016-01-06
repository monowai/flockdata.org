/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
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
        ClientConfiguration configuration = getClientConfiguration(profile);
        FileProcessor fileProcessor = new FileProcessor();
        ImportProfile params = ClientConfiguration.getImportProfile(profile);
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
