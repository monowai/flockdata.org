/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.mvc;

import org.flockdata.helper.JsonUtils;
import org.flockdata.profile.*;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Created by mike on 14/04/16.
 */
public class TestContentProfile extends  MvcBase{
    @Test
    public void testSaveRetrieveContent() throws  Exception {
        ContentProfileImpl contentProfile = ContentProfileDeserializer.getContentProfile("/profiles/test-csv-batch.json");
        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        Collection<DocumentTypeInputBean> documents = new ArrayList<>();
        documents.add(new DocumentTypeInputBean("ContentStore"));
        makeDocuments(mike(), fortressResultBean, documents  );
        ContentProfileResult result = makeContentProfile(mike(),
                    fortressResultBean.getCode(),
                    "ContentStore",
                    contentProfile,
                    MockMvcResultMatchers.status().isOk());
        assertNotNull ( result);

        ContentProfile contentResult = getContentProfile(mike(),
                fortressResultBean.getCode(),
                "ContentStore",
                contentProfile,
                MockMvcResultMatchers.status().isOk());

        assertNotNull ( contentResult);
        assertNull (contentResult.getPreParseRowExp());
        assertEquals("Content Profiles differed", contentProfile, contentResult);
    }

    @Test
    public void validate_Profile() throws  Exception {
        makeDataAccessProfile("validateContentProfile", "mike");
        ContentProfile profile = ContentProfileDeserializer.getContentProfile("/profiles/test-profile.json");
        ContentValidationRequest validationRequest = new ContentValidationRequest(profile);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull (json);
        assertNotNull (JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class).getContentProfile());
        ContentValidationResults result = validateContent(mike(), validationRequest, MockMvcResultMatchers.status().isOk());
        assertNotNull (result);
        assertFalse ( result.getResults().isEmpty());

    }


}
