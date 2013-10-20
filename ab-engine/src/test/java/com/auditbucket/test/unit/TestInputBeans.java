/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.test.unit;

import com.auditbucket.audit.model.TxRef;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * User: Mike Holdsworth
 * Date: 22/06/13
 * Time: 5:02 AM
 */
public class TestInputBeans {
    private Logger log = LoggerFactory.getLogger(TestInputBeans.class);

    @Test
    public void testTxStatus() {
        TxRefNode tx = new TxRefNode("abc", new CompanyNode(""));

        // Current status should be created
        assertEquals(TxRef.TxStatus.TX_CREATED, tx.getTxStatus());
        TxRefNode.TxStatus previous = tx.commit();
        assertEquals(TxRef.TxStatus.TX_COMMITTED, tx.getTxStatus());
        assertEquals(TxRef.TxStatus.TX_CREATED, previous);
        previous = tx.rollback();
        assertEquals(TxRef.TxStatus.TX_ROLLBACK, tx.getTxStatus());
        assertEquals(TxRef.TxStatus.TX_COMMITTED, previous);

    }

    @Test
    public void testFortressInputBean() {
        CompanyNode c = new CompanyNode("test");

        FortressInputBean fib = new FortressInputBean("ABC");
        assertFalse(fib.getIgnoreSearch());

        fib = new FortressInputBean("ABC", false);
        assertFalse(fib.getIgnoreSearch());

        fib = new FortressInputBean("ABC", true);
        assertTrue(fib.getIgnoreSearch());

        fib = new FortressInputBean("ABC", false);
        assertFalse(fib.getIgnoreSearch());

    }

    @Test
    public void testAuditInputBean() throws Exception {
        AuditHeaderInputBean aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now(), "myRef");
        assertNull(aib.getAuditKey());
        aib.setAuditKey("AbC");
        assertNotNull(aib.getAuditKey());

        // NonNull tx ref sets the inputBean to be transactional
        String what = "{\"abc\":0}";
        AuditLogInputBean alb = new AuditLogInputBean("aaa", "user", null, what, "", "txreftest");
        assertTrue(alb.isTransactional());

        alb = new AuditLogInputBean("aaa", "user", null, what);
        assertFalse(alb.isTransactional());


    }


}
