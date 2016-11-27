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

package org.flockdata.test.engine.services;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.Store;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Version strategies
 * @author mholdsworth
 * @since 16/01/2016
 */
public class TestVersionedDocuments extends EngineBase {

    @Test
    public void defaults_ByDocumentType() throws Exception {

        // Document properties can be overridden from the fortress default
        SystemUser su = registerSystemUser("defaults_ByDocumentType");

        Fortress fortress= fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("DocTypeTest", true)
                .setStoreEnabled(true));

        assertTrue(fortress.isStoreEnabled());

        DocumentType documentType = conceptService.findOrCreate(fortress,
                new DocumentType(fortress.getDefaultSegment(),
                        new DocumentTypeInputBean("Default")
                                .setCode("Default")));

        assertEquals("Basic default is not being honoured", DocumentType.VERSION.FORTRESS,documentType.getVersionStrategy());
        documentType.setVersionStrategy(DocumentType.VERSION.DISABLE);
        documentType = conceptService.save(documentType);
        TestCase.assertEquals("Update of version strategy property not working", DocumentType.VERSION.DISABLE, documentType.getVersionStrategy());
    }


    @Test
    public void trackResult_Kv() throws Exception {

        // Check that the same fortress can have DocTypes with kv stores selectively enabled
        SystemUser su = registerSystemUser("trackResult_Kv");

        Fortress fortress= fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("trackResult_Kv", true)
                        .setStoreEnabled(true));

        assertTrue(fortress.isStoreEnabled());

        DocumentType memoryType = conceptService.findOrCreate(fortress,
                new DocumentType(fortress.getDefaultSegment(),
                        new DocumentTypeInputBean("Memory")
                                .setCode("Memory"))
        .setVersionStrategy(DocumentType.VERSION.FORTRESS));

        EntityInputBean eib = new EntityInputBean(fortress, new DocumentTypeInputBean(memoryType.getName()))
                .setCode("ABC");

        TrackResultBean memResultBean = mediationFacade.trackEntity(su.getCompany(), eib);

        Store kvStore = Store.resolveStore(memResultBean, Store.MEMORY);

        TestCase.assertEquals( Store.MEMORY, kvStore);
        DocumentTypeInputBean documentTypeInputBean =new DocumentTypeInputBean("None")
                .setCode("None")
                .setVersionStrategy (DocumentType.VERSION.DISABLE);

        // Validate JSON serialization
        String json = JsonUtils.toJson(documentTypeInputBean);
        documentTypeInputBean = JsonUtils.toObject(json.getBytes(), DocumentTypeInputBean.class);

        TestCase.assertEquals(DocumentType.VERSION.DISABLE, documentTypeInputBean.getVersionStrategy());

        DocumentType noneType = conceptService.findOrCreate(fortress,
                new DocumentType(fortress.getDefaultSegment(), documentTypeInputBean));

        TestCase.assertEquals(DocumentType.VERSION.DISABLE, noneType.getVersionStrategy());

        eib = new EntityInputBean(fortress, new DocumentTypeInputBean(noneType.getName()))
                .setCode("CBA");

        TrackResultBean noneResultBean = mediationFacade.trackEntity(su.getCompany(), eib);
        kvStore = Store.resolveStore(noneResultBean, Store.NONE);
        TestCase.assertEquals( Store.NONE, kvStore);

    }
}
