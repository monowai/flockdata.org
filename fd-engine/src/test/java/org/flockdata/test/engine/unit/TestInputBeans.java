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

package org.flockdata.test.engine.unit;

import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.TxRef;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author mholdsworth
 * @since 22/06/2013
 */
public class TestInputBeans {
    //private Logger log = LoggerFactory.getLogger(TestInputBeans.class);

    @Test
    public void testTxStatus() {
        org.flockdata.model.TxRef tx = new TxRef("abc", new Company(""));

        // Current status should be created
        assertEquals(TxRef.TxStatus.TX_CREATED, tx.getTxStatus());
        TxRef.TxStatus previous = tx.commit();
        assertEquals(TxRef.TxStatus.TX_COMMITTED, tx.getTxStatus());
        assertEquals(TxRef.TxStatus.TX_CREATED, previous);
        previous = tx.rollback();
        assertEquals(TxRef.TxStatus.TX_ROLLBACK, tx.getTxStatus());
        assertEquals(TxRef.TxStatus.TX_COMMITTED, previous);

    }

    @Test
    public void testFortressInputBean() {

        FortressInputBean fib = new FortressInputBean("ABC");
        assertEquals (null, fib.getSearchEnabled());
        assertEquals (null, fib.getStoreEnabled());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchEnabled());

        fib = new FortressInputBean("ABC", true);
        assertFalse(fib.getSearchEnabled());

        fib = new FortressInputBean("ABC", false);
        assertTrue(fib.getSearchEnabled());

        fib.setStoreEnabled(false);
        assertFalse(fib.getStoreEnabled());

        fib.setStoreEnabled(null);
        assertEquals (null, fib.getStoreEnabled());
    }

    @Test
    public void testEntityInputBean() throws Exception {
        DateTime now = DateTime.now();
        Fortress fortress = new Fortress(new FortressInputBean("fortress"), new Company("blah"));
        EntityInputBean entityBean = new EntityInputBean(fortress, "user", "booking", now, "myRef");
        assertNull(entityBean.getKey());
        entityBean.setKey("AbC");
        assertNotNull(entityBean.getKey());

        // NonNull tx ref sets the inputBean to be transactional
        DateTime logNow = DateTime.now();
        ContentInputBean logBean = new ContentInputBean("user", "aaa", logNow, EntityContentHelper.getSimpleMap("abc", 0), "", "txreftest");
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

        logBean = new ContentInputBean("user", "aaa", null, EntityContentHelper.getRandomMap());
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
        TagInputBean tag = new TagInputBean("SimpleName");
        tag.addEntityTagLink("myrlx");
        assertFalse ( tag.getEntityTagLinks().isEmpty());
        assertTrue(tag.getEntityTagLinks().containsKey("myrlx"));
    }


}
