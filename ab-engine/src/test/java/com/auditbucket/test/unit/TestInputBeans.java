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

package com.auditbucket.test.unit;

import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.ConceptInputBean;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.TxRef;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Date: 22/06/13
 * Time: 5:02 AM
 */
public class TestInputBeans {
    //private Logger log = LoggerFactory.getLogger(TestInputBeans.class);

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

        FortressInputBean fib = new FortressInputBean("ABC");
        assertTrue(fib.getSearchActive());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

        fib = new FortressInputBean("ABC", true);
        assertFalse(fib.getSearchActive());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

    }

    @Test
    public void testTrackInputBean() throws Exception {
        DateTime headerNow = DateTime.now();
        MetaInputBean metaBean = new MetaInputBean("fortress", "user", "booking", headerNow, "myRef");
        assertNull(metaBean.getMetaKey());
        metaBean.setMetaKey("AbC");
        assertNotNull(metaBean.getMetaKey());

        // NonNull tx ref sets the inputBean to be transactional
        DateTime logNow = DateTime.now();
        LogInputBean logBean = new LogInputBean("user", "aaa", logNow, TestHelper.getSimpleMap("abc", 0), "", "txreftest");
        metaBean.setLog(logBean); // Creation dates defer to the Log
        assertTrue(logBean.isTransactional());
        assertEquals(headerNow.getMillis(), metaBean.getWhen().getTime());

        // Change the date on the log, should be the same in the header
        logBean.setWhen(headerNow.toDate());
        assertEquals(headerNow.getMillis(), metaBean.getWhen().getTime());
        // Null the log
        metaBean.setLog(null);
        Date dateC = new Date();
        logBean.setWhen(dateC);
        metaBean.setLog(logBean);
        assertEquals(dateC.getTime(), logBean.getWhen().getTime());
        assertNotSame(dateC.getTime(), metaBean.getWhen().getTime());
        assertEquals(headerNow.getMillis(), metaBean.getWhen().getTime());

        logBean = new LogInputBean("user", "aaa", null, TestHelper.getRandomMap());
        assertFalse(logBean.isTransactional());


    }

    @Test
    public void valuesDefaultCorrectly(){
        TagInputBean tib = new TagInputBean("Hello");
        assertEquals("Hello", tib.getCode());
        tib.setCode("hello");
        assertEquals("hello", tib.getCode());
        tib.setIndex("Testing");
        assertEquals("Testing", tib.getIndex());
    }

    @Test
    public void mergeTags(){
        TagInputBean dest   = new TagInputBean("hello");
        TagInputBean source = new TagInputBean("hello");

        TagInputBean target = new TagInputBean("target");
        TagInputBean other = new TagInputBean("other");

        //source.addMetaLink("abc");
        source.setTargets("somerlx", target);
        source.setTargets("otherrlx", target);

        dest.setTargets("somerlx", other );// This one appends to somerlx collection

        dest.mergeTags(source);
        //assertEquals(1, dest.getMetaLinks().size());
        assertEquals("Should be 2 relationships", 2, dest.getTargets().size());
        assertEquals("TagInput did not merge into somerlx", 2, dest.getTargets().get("somerlx").size());
    }

    @Test
    public void metaLinksFromInput(){
        ConceptInputBean cib = new ConceptInputBean();
        TagInputBean tag = new TagInputBean("SimpleName");
        tag.addMetaLink("myrlx");
        assertFalse ( tag.getMetaLinks().isEmpty());
        assertTrue(tag.getMetaLinks().containsKey("myrlx"));
    }


}
