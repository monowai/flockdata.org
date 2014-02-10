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

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.TxRef;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
        assertFalse(fib.getSearchActive());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

        fib = new FortressInputBean("ABC", true);
        assertFalse(fib.getSearchActive());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

    }

    @Test
    public void testAuditInputBean() throws Exception {
        DateTime dateA = DateTime.now();
        AuditHeaderInputBean aib = new AuditHeaderInputBean("fortress", "user", "booking", dateA, "myRef");
        assertNull(aib.getAuditKey());
        aib.setAuditKey("AbC");
        assertNotNull(aib.getAuditKey());


        // NonNull tx ref sets the inputBean to be transactional
        String what = "{\"abc\":0}";
        DateTime dateB = DateTime.now();
        AuditLogInputBean alb = new AuditLogInputBean("aaa", "user", dateB, what, "", "txreftest");
        aib.setAuditLog(alb); // Creation dates defer to the Log
        assertTrue(alb.isTransactional());
        assertEquals(dateB.getMillis(), aib.getWhen().getTime());
        // Try to override the
        aib.setWhen(DateTime.now().toDate());
        assertEquals(dateB.getMillis(), aib.getWhen().getTime());
        assertEquals(aib.getWhen().getTime(), alb.getWhen().getTime());

        // Change the date on the log, should be the same in the header
        alb.setWhen(dateA.toDate());
        assertEquals(dateA.getMillis(), aib.getWhen().getTime());
        // Null the log
        aib.setAuditLog(null);
        Date dateC = new Date();
        alb.setWhen(dateC);
        aib.setAuditLog(alb);
        assertEquals(dateC.getTime(), aib.getWhen().getTime());

        alb = new AuditLogInputBean("aaa", "user", null, what);
        assertFalse(alb.isTransactional());


    }

    @Test
    public void labeledTagInputBreaksOut(){
        String input ="Name:Label";
        TagInputBean t = new TagInputBean(input);
        assertEquals(":Label", t.getIndex());
        assertEquals("Name", t.getName());

        input = "Name:LabelA:LabelB";
        t = new TagInputBean(input);
        assertEquals(":LabelA :LabelB", t.getIndex());
        assertEquals("Name", t.getName());

        input = "Name";
        t = new TagInputBean(input);
        assertEquals("Name", t.getName());
        assertEquals("", t.getIndex());
        try {
            new TagInputBean("Hello There", "White Space Not Allowed");
            fail("Whitespace is not allowed in a tag type");
            new TagInputBean("Hello There:White Space Not Allowed");
            fail("Whitespace is not allowed in a tag type");
        } catch (Exception e ){
            // This is good
        }

    }

    @Test
    public void valuesDefaultCorrectly(){
        TagInputBean tib = new TagInputBean("Hello");
        assertEquals("Hello", tib.getCode());
        tib.setCode("hello");
        assertEquals("hello", tib.getCode());
        tib.setIndex("Testing");
        assertEquals(":Testing", tib.getIndex());
    }


}
