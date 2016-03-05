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

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.shared.FileProcessor;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 20/01/16.
 */
public class TestDocTypes extends AbstractImport  {
    @Autowired
    FileProcessor fileProcessor  ;


    @Test
    public void testDocType() throws Exception{

        ContentProfileImpl contentProfile = ProfileReader.getImportProfile("/profile/test-document-type.json");

        fileProcessor.processFile(contentProfile, "/data/pac.txt");

        for (EntityInputBean entityInputBean : fdBatcher.getEntities()) {
            DocumentTypeInputBean docType = entityInputBean.getDocumentType();
            assertNotNull(entityInputBean.getDocumentType());
            TestCase.assertEquals("TestDocType", docType.getName());
            TestCase.assertEquals("Version Strategy not being handled", DocumentType.VERSION.ENABLE, docType.getVersionStrategy());
            EntityService.TAG_STRUCTURE tagStructure = docType.getTagStructure();
            TestCase.assertEquals("Unable to read the custom taxonomy structure", EntityService.TAG_STRUCTURE.TAXONOMY, tagStructure);
        }
    }

    @Test
    public void defaults_Serialize() throws Exception{
        // Fundamental assertions are the payload is serialized

        DocumentTypeInputBean dib = new DocumentTypeInputBean("MyDoc")
                .setTagStructure(EntityService.TAG_STRUCTURE.TAXONOMY)
                .setVersionStrategy(DocumentType.VERSION.DISABLE);

        Company company = new Company("CompanyName");
        Fortress fortress = new Fortress(new FortressInputBean("FortressName"),company)
                .setSearchEnabled(true);

        byte[] bytes = JsonUtils.toJsonBytes(dib);
        assertEquals(dib.getTagStructure(), JsonUtils.toObject(bytes,DocumentTypeInputBean.class).getTagStructure() );

        EntityInputBean compareFrom = new EntityInputBean(fortress, dib);
        assertEquals(dib.getTagStructure(), compareFrom.getDocumentType().getTagStructure());

        EntityInputBean deserialize
                = JsonUtils.toObject(JsonUtils.toJsonBytes(compareFrom), EntityInputBean.class);
        assertNotNull (deserialize);

        assertEquals(compareFrom.getDocumentType().getCode(), deserialize.getDocumentType().getCode());
        assertEquals(compareFrom.getDocumentType().getTagStructure(), deserialize.getDocumentType().getTagStructure());
        assertEquals(compareFrom.getDocumentType().getVersionStrategy(), deserialize.getDocumentType().getVersionStrategy());

    }
}
