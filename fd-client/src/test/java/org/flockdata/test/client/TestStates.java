/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.ClientConfiguration;
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
