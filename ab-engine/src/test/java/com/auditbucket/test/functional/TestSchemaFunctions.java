/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.model.DocumentType;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:52 AM
 */
@Transactional
public class TestSchemaFunctions extends TestEngineBase {

    String company = "TestSchema Ltd.";
    String uid = "anyuser@testschema.com";

    @Test
    public void documentTypesWork() throws Exception {
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(company, uid));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("ABC", true), su.getApiKey(), su.getApiKey()).getBody();

        String docName = "CamelCaseDoc";
        DocumentType docType = schemaService.resolveDocType(fortress, docName); // Creates if missing
        assertNotNull(docType);
        assertEquals(docName.toLowerCase(), docType.getCode());
        assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        Assert.assertNotNull(schemaService.resolveDocType(fortress, docType.getCode().toUpperCase(), false));

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        setSecurity(sally);
        SystemUser su = regService.registerSystemUser(new RegistrationBean(company, sally));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortA");
        Fortress fortB = fortressService.registerFortress("fortB");

        DocumentType dType = schemaService.resolveDocType(fortA, "ABC123", true);
        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortA, "ABC123", false);
        assertEquals(id, dType.getId());

        DocumentType nextType = schemaService.resolveDocType(fortB, "ABC123", true);
        Assert.assertNotSame("Same company + different fortresses = different document types", dType, nextType);

        // Company 2 gets a different tag with the same name
        setSecurity(harry); // Register an Auth user as an engine system user
        regService.registerSystemUser(new RegistrationBean("secondcompany", harry));
        // Same fortress name, but different company results in a new fortress
        dType = schemaService.resolveDocType(fortressService.registerFortress("fortA"), "ABC123"); // Creates if missing
        Assert.assertNotNull(dType);
        Assert.assertNotSame(id, dType.getId());
    }
}
