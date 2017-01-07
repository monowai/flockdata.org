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

import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.EntityTag;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

/**
 * @author mholdsworth
 * @since 20/01/2016
 */
public class TestDocTypes extends AbstractImport  {

    @Test
    public void testDocType() throws Exception{

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/test-document-type.json");

        fileProcessor.processFile(new ExtractProfileHandler(contentModel, false), "/data/pac.txt");

        for (EntityInputBean entityInputBean : fdTemplate.getEntities()) {
            Document docType = entityInputBean.getDocumentType();
            assertNotNull(entityInputBean.getDocumentType());
            TestCase.assertEquals("TestDocType", docType.getName());
            TestCase.assertEquals("Version Strategy not being handled", Document.VERSION.ENABLE, docType.getVersionStrategy());
            EntityTag.TAG_STRUCTURE tagStructure = docType.getTagStructure();
            TestCase.assertEquals("Unable to read the custom taxonomy structure", EntityTag.TAG_STRUCTURE.TAXONOMY, tagStructure);
        }
    }


}
