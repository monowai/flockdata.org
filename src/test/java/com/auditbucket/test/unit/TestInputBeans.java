package com.auditbucket.test.unit;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.repo.neo4j.model.TxRef;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.test.functional.TestAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 22/06/13
 * Time: 5:02 AM
 */
public class TestInputBeans {
    private Log log = LogFactory.getLog(TestAudit.class);

    @Test
    public void testTxStatus() {
        TxRef tx = new TxRef("abc", new Company(""));

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
        Company c = new Company("test");

        FortressInputBean fib = new FortressInputBean("ABC");
        assertTrue(fib.getIgnoreSearchEngine());
        assertFalse(fib.getAccumulateChanges());
        fib.setAccumulateChanges(true);
        // Setting accumulate changes turns on search engine tracking
        assertTrue(fib.getAccumulateChanges());
        assertFalse(fib.getIgnoreSearchEngine());


        Fortress f = new Fortress(fib, c);
        assertTrue(f.isAccumulatingChanges());
        assertFalse(f.isIgnoreSearchEngine());

        //AuditHeader ah = new AuditHeader()

    }

    @Test
    public void testAuditInputBean() {
        AuditHeaderInputBean aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now().toDate(), "myRef");
        assertNull(aib.getAuditKey());
        assertFalse(aib.isTransactional());
        aib.setAuditKey("AbC");
        assertNotNull(aib.getAuditKey());

        aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now().toDate(), "myRef", true);
        assertTrue(aib.isTransactional());
        assertNull(aib.getTxRef());

        // NonNull tx ref sets the inputBean to be transactional
        aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now().toDate(), "myRef", "txref");
        assertTrue(aib.isTransactional());

        aib = new AuditHeaderInputBean("fortress", "user", "booking", DateTime.now().toDate(), "myRef", (String) null);
        assertFalse(aib.isTransactional());

    }

    @Test
    public void testEscapedJson() {
        String test = JSONValue.escape("{\"name\": \"value\"}");
        JSONValue.isValidJson(test);
        ObjectMapper om = new ObjectMapper();


    }

}
