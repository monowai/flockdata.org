package org.flockdata.test.search;

import junit.framework.TestCase;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.IndexManager;
import org.junit.Test;

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
        String[] indexes = indexManager.getIndexesToQuery(company, fortress, types, segment);
        TestCase.assertEquals(1, indexes.length);
        TestCase.assertEquals("fd.flockdata.fdm.lar.2013", indexes[0]);
    }

    @Test
    public void compute_segmentWildCard() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "FlockData";
        String fortress = "fdm";
        String segment = null;
        String types[] = new String[1];

        String[] indexes = indexManager.getIndexesToQuery(company, fortress, types, segment);
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
        String[] indexes = indexManager.getIndexesToQuery(company, fortress, types, segment);
        int count=0 ;
        for (String index : indexes) {
            validateIndex(company, fortress, types[count], segment, index);
            count++;
        }


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
        TestCase.assertEquals("If this fails then locating the content when KV_NONE will fail", "fd." + company.toLowerCase() + "." + fortress.toLowerCase() + "." + segment.toLowerCase(), parsedIndex);
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
        int expectedCount = 2; // We set two type filters so should be at least two indexes
        TestCase.assertEquals(expectedCount, indexes.length);
        int count =0;
        for (String index : indexes) {
            if ( count == 0)
                validateIndex(company, fortress, "type0", segment, index);
            else
                validateIndex(company, fortress, "type1", segment, index);
            count ++;
        }

    }

    @Test
    public void wildCardFortress() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);

        String[] indexes = indexManager.getIndexesToQuery(qp);
        TestCase.assertEquals(1, indexes.length);
        for (String index : indexes) {
            assertTrue(index.startsWith(indexManager.getPrefix() + company.toLowerCase() + ".*"));
        }
    }

    @Test
    public void blah () throws Exception{
        QueryParams qp = new QueryParams("*");
        qp.setFortress("theFortress");
        qp.setCompany("theCompany"); // normally this is set automatically by fd-engine
        qp.setTypes("thedoc");
        IndexManager indexManager = new IndexManager("fd.", true);
        String indexes[] = indexManager.getIndexesToQuery(qp);
        for (String index : indexes) {
            System.out.println(index);
        }

    }
    /**
     * Validates that the index matches a valid ElasticSearch structure
     *
     * @param company
     * @param fortress
     * @param segment
     * @param index
     * @throws Exception
     */
    private void validateIndex(String company, String fortress, String type, String segment, String index) throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        assertNotNull(index);

        TestCase.assertEquals(index,
                indexManager.getPrefix() +
                        company.toLowerCase() + "." +
                        fortress.toLowerCase() +
                        (type == null ? "":"." + type.toLowerCase()) +
                        (segment == null ? "*" : "." + segment.toLowerCase())
        );
    }


}
