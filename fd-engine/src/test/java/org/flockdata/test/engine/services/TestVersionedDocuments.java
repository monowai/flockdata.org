/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.test.engine.services;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

import junit.framework.TestCase;
import org.flockdata.data.Document;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.Store;
import org.flockdata.store.StoreHelper;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.junit.Test;

/**
 * Version strategies
 *
 * @author mholdsworth
 * @since 16/01/2016
 */
public class TestVersionedDocuments extends EngineBase {

    @Test
    public void defaults_ByDocumentType() throws Exception {

        // Document properties can be overridden from the fortress default
        SystemUser su = registerSystemUser("defaults_ByDocumentType");

        FortressNode fortress = fortressService.registerFortress(su.getCompany(),
            new FortressInputBean("DocTypeTest", true)
                .setStoreEnabled(true));

        assertTrue(fortress.isStoreEnabled());

        DocumentNode documentType = conceptService.findOrCreate(fortress,
            new DocumentNode(fortress.getDefaultSegment(),
                new DocumentTypeInputBean("Default")
                    .setCode("Default")));

        assertEquals("Basic default is not being honoured", Document.VERSION.FORTRESS, documentType.getVersionStrategy());
        documentType.setVersionStrategy(Document.VERSION.DISABLE);
        documentType = conceptService.save(documentType);
        TestCase.assertEquals("Update of version strategy property not working", Document.VERSION.DISABLE, documentType.getVersionStrategy());
    }


    @Test
    public void trackResult_Kv() throws Exception {

        // Check that the same fortress can have DocTypes with kv stores selectively enabled
        SystemUser su = registerSystemUser("trackResult_Kv");

        FortressNode fortress = fortressService.registerFortress(su.getCompany(),
            new FortressInputBean("trackResult_Kv", true)
                .setStoreEnabled(true));

        assertTrue(fortress.isStoreEnabled());

        DocumentNode memoryType = conceptService.findOrCreate(fortress,
            new DocumentNode(fortress.getDefaultSegment(),
                new DocumentTypeInputBean("Memory")
                    .setCode("Memory"))
                .setVersionStrategy(Document.VERSION.FORTRESS));

        EntityInputBean eib = new EntityInputBean(fortress, new DocumentTypeInputBean(memoryType.getName()))
            .setCode("ABC");

        TrackResultBean memResultBean = mediationFacade.trackEntity(su.getCompany(), eib);

        Store kvStore = StoreHelper.resolveStore(memResultBean, Store.MEMORY);

        TestCase.assertEquals(Store.MEMORY, kvStore);
        Document documentTypeInputBean = new DocumentTypeInputBean("None")
            .setCode("None")
            .setVersionStrategy(Document.VERSION.DISABLE);

        // Validate JSON serialization
        String json = JsonUtils.toJson(documentTypeInputBean);
        documentTypeInputBean = JsonUtils.toObject(json.getBytes(), DocumentTypeInputBean.class);

        TestCase.assertEquals(Document.VERSION.DISABLE, documentTypeInputBean.getVersionStrategy());

        DocumentNode noneType = conceptService.findOrCreate(fortress,
            new DocumentNode(fortress.getDefaultSegment(), documentTypeInputBean));

        TestCase.assertEquals(Document.VERSION.DISABLE, noneType.getVersionStrategy());

        eib = new EntityInputBean(fortress, new DocumentTypeInputBean(noneType.getName()))
            .setCode("CBA");

        TrackResultBean noneResultBean = mediationFacade.trackEntity(su.getCompany(), eib);
        kvStore = StoreHelper.resolveStore(noneResultBean, Store.NONE);
        TestCase.assertEquals(Store.NONE, kvStore);

    }
}
