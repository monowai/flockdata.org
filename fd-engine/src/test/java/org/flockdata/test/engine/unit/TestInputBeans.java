/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.unit;

import org.flockdata.company.model.CompanyNode;
import org.flockdata.engine.schema.model.TxRefNode;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.TxRef;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

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
        assertEquals (null, fib.getVersioning());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

        fib = new FortressInputBean("ABC", true);
        assertFalse(fib.getSearchActive());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchActive());

        fib.setVersioning(false);
        assertFalse(fib.getVersioning());

        fib.setVersioning(null);
        assertEquals (null, fib.getVersioning());
    }

    @Test
    public void testEntityInputBean() throws Exception {
        DateTime now = DateTime.now();
        EntityInputBean entityBean = new EntityInputBean("fortress", "user", "booking", now, "myRef");
        assertNull(entityBean.getMetaKey());
        entityBean.setMetaKey("AbC");
        assertNotNull(entityBean.getMetaKey());

        // NonNull tx ref sets the inputBean to be transactional
        DateTime logNow = DateTime.now();
        ContentInputBean logBean = new ContentInputBean("user", "aaa", logNow, Helper.getSimpleMap("abc", 0), "", "txreftest");
        entityBean.setContent(logBean); // Creation dates defer to the Log
        assertTrue(logBean.isTransactional());
        assertEquals(now.getMillis(), entityBean.getWhen().getTime());

        // Change the date on the log, should be the same in the entity
        logBean.setWhen(now.toDate());
        assertEquals(now.getMillis(), entityBean.getWhen().getTime());
        // Null the log
        entityBean.setContent(null);
        Date dateC = new Date();
        logBean.setWhen(dateC);
        entityBean.setContent(logBean);
        assertEquals(dateC.getTime(), logBean.getWhen().getTime());
        assertNotSame(dateC.getTime(), entityBean.getWhen().getTime());
        assertEquals(now.getMillis(), entityBean.getWhen().getTime());

        logBean = new ContentInputBean("user", "aaa", null, Helper.getRandomMap());
        assertFalse(logBean.isTransactional());


    }

    @Test
    public void valuesDefaultCorrectly(){
        TagInputBean tib = new TagInputBean("Hello");
        assertEquals("Hello", tib.getCode());
        tib.setCode("hello");
        assertEquals("hello", tib.getCode());
        tib.setLabel("Testing");
        assertEquals("Testing", tib.getLabel());
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
        //assertEquals(1, dest.getEntityLinks().size());
        assertEquals("Should be 2 relationships", 2, dest.getTargets().size());
        assertEquals("TagInput did not merge into somerlx", 2, dest.getTargets().get("somerlx").size());
    }

    @Test
    public void metaLinksFromInput(){
        ConceptInputBean cib = new ConceptInputBean();
        TagInputBean tag = new TagInputBean("SimpleName");
        tag.addEntityLink("myrlx");
        assertFalse ( tag.getEntityLinks().isEmpty());
        assertTrue(tag.getEntityLinks().containsKey("myrlx"));
    }


}
