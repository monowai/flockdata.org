package org.flockdata.test.search;

import junit.framework.TestCase;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.IndexManager;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Indexes to query are computed at runtime. This validates the generic functionality that
 * composes a valid ES index path
 * <p/>
 * Created by mike on 23/07/15.
 */

public class TestIndexHelper {

    @Test
    public void compute_segmentIndex() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "FlockData";
        String fortress = "fdm";
        String segment = "2013";
        String types[] = new String[1];

        types[0] = "lar";
        String[] indexes = indexManager.getIndexesToQuery(company, fortress, segment, types);
        TestCase.assertEquals(1, indexes.length);
        TestCase.assertEquals("fd.flockdata.fdm.2013", indexes[0]);
    }

    @Test
    public void compute_segmentWildCard() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "FlockData";
        String fortress = "fdm";
        String segment = null;
        String types[] = new String[1];

        types[0] = "lar";
        String[] indexes = indexManager.getIndexesToQuery(company, fortress, segment, types);
        TestCase.assertEquals(1, indexes.length);
        TestCase.assertEquals("fd.flockdata.fdm*", indexes[0]);
    }

    @Test
    public void testA() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        String types[] = new String[2];

        types[0] = "Type0";
        types[1] = "Type1";
        String[] indexes = indexManager.getIndexesToQuery(company, fortress, segment, types);
        validateIndexesForQuery(company, fortress, segment, indexes);

    }

    //
    @Test
    public void kvStoreRetrievalIndex() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        qp.setFortress(fortress);
        qp.setSegment(segment);
        qp.setTypes("Type0");
        String parsedIndex = indexManager.parseIndex(qp);
        TestCase.assertEquals("If this fails then locating the content when KV_NONE will fail","fd."+company.toLowerCase()+"."+fortress.toLowerCase()+"."+segment.toLowerCase(), parsedIndex);
        //String[] indexes = IndexHelper.getIndexesToQuery(qp);
        //validateIndexesForQuery(company, fortress, segment, indexes);
    }

    @Test
    public void testFromQueryParams() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        qp.setFortress(fortress);
        qp.setSegment(segment);
        qp.setTypes("Type0", "type1");

        String[] indexes = indexManager.getIndexesToQuery(qp);
        validateIndexesForQuery(company, fortress, segment, indexes);
    }

    @Test
    public void wildCardFortress() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        //String fortress = "123";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        //qp.setFortress(fortress);
        qp.setTypes("Type0", "type1");

        String[] indexes = indexManager.getIndexesToQuery(qp);
        TestCase.assertEquals(1, indexes.length);
        for (String index : indexes) {
            assertTrue(index.startsWith(indexManager.getPrefix() + company.toLowerCase() + ".*"));
        }
        //validateIndexes(company, fortress, indexes);
    }

    /**
     * Validates that the index matches a valid ElasticSearch structure
     *
     * @param company
     * @param fortress
     * @param segment
     * @param indexes
     * @throws Exception
     */
    private void validateIndexesForQuery(String company, String fortress, String segment, String[] indexes) throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        assertNotNull(indexes);
        assertEquals(1, indexes.length);
        int foundCount = 0;
        for (String index : indexes) {

            // Prefix.company.fortress.segment
            if (index.equals(indexManager.getPrefix() + company.toLowerCase() + "." + fortress.toLowerCase() + (segment == null ? "*" : "." + segment.toLowerCase()))) {
                foundCount++;
            }
        }
        assertTrue("Incorrect found count", foundCount == indexes.length);

    }


}
