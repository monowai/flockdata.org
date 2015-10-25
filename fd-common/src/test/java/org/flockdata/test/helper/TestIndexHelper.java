package org.flockdata.test.helper;

import junit.framework.TestCase;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.model.QueryParams;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.*;

/**
 * Created by mike on 23/07/15.
 */
public class TestIndexHelper {
    @Test
    public void testA () throws Exception {
        String company = "abc";
        String fortress = "123";
        String types[] = new String[2];

        types[0]="Type0";
        types[1]="Type1";
        String[] indexes = IndexHelper.getIndexesToQuery(company, fortress, types);
        validateIndexes(company, fortress, indexes);
    }

    @Test
    public void testFromQueryParams () throws Exception {
        String company = "abc";
        String fortress = "123";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        qp.setFortress(fortress);
        qp.setTypes("Type0", "type1");

        String[] indexes = IndexHelper.getIndexesToQuery(qp);
        validateIndexes(company, fortress, indexes);
    }

    @Test
    public void wildCardFortress () throws Exception {
        String company = "abc";
        //String fortress = "123";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        //qp.setFortress(fortress);
        qp.setTypes("Type0", "type1");

        String[] indexes = IndexHelper.getIndexesToQuery(qp);
        TestCase.assertEquals(1, indexes.length);
        for (String index : indexes) {
            assertTrue(index.startsWith(IndexHelper.PREFIX+company.toLowerCase()+".*"));
        }
        //validateIndexes(company, fortress, indexes);
    }

    private void validateIndexes(String company, String fortress, String[] indexes) throws Exception {
        assertNotNull(indexes);
        assertEquals(1, indexes.length);
        int count = 0;
        int foundCount = 0;
        for (String index : indexes) {
            if ( index.equals(IndexHelper.PREFIX+company.toLowerCase()+"."+fortress.toLowerCase())) {
                foundCount++;
                count++;
            }
        }
        assertTrue("Incorrect found count", foundCount == indexes.length);

    }


}
