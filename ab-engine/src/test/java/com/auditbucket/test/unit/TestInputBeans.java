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

import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.core.registration.bean.FortressInputBean;
import com.auditbucket.core.registration.repo.neo4j.model.Company;
import com.auditbucket.core.repo.neo4j.model.TxRef;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 22/06/13
 * Time: 5:02 AM
 */
public class TestInputBeans {
    private Logger log = LoggerFactory.getLogger(TestInputBeans.class);

    @Test
    public void testTxStatus() {
        TxRef tx = new TxRef("abc", new Company(""));

        // Current status should be created
        assertEquals(ITxRef.TxStatus.TX_CREATED, tx.getTxStatus());
        TxRef.TxStatus previous = tx.commit();
        assertEquals(ITxRef.TxStatus.TX_COMMITTED, tx.getTxStatus());
        assertEquals(ITxRef.TxStatus.TX_CREATED, previous);
        previous = tx.rollback();
        assertEquals(ITxRef.TxStatus.TX_ROLLBACK, tx.getTxStatus());
        assertEquals(ITxRef.TxStatus.TX_COMMITTED, previous);

    }

    @Test
    public void testFortressInputBean() {
        Company c = new Company("test");

        FortressInputBean fib = new FortressInputBean("ABC");
        assertFalse(fib.getIgnoreSearchEngine());
        assertFalse(fib.getAccumulateChanges());
        fib.setAccumulateChanges(true);
        // Setting accumulate changes turns on search engine tracking
        assertTrue(fib.getAccumulateChanges());
        assertFalse(fib.getIgnoreSearchEngine());

        fib = new FortressInputBean("ABC", false);
        assertFalse(fib.getAccumulateChanges());
        assertFalse(fib.getIgnoreSearchEngine());

        fib = new FortressInputBean("ABC", true);
        assertTrue(fib.getAccumulateChanges());
        assertFalse(fib.getIgnoreSearchEngine());

        fib = new FortressInputBean("ABC", false, false);
        assertFalse(fib.getAccumulateChanges());
        assertFalse(fib.getIgnoreSearchEngine());

    }

    @Test
    public void testAuditInputBean() throws Exception {
        AuditHeaderInputBean aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now().toDate(), "myRef");
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
