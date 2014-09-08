package com.auditbucket.test.functional;

import com.auditbucket.search.helper.QueryGenerator;
import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: macpro
 * Date: 04/08/2014
 * Time: 15:31
 * To change this template use File | Settings | File Templates.
 */
public class QueryGeneratorTest {

    @Test
    public void testGetSimpleQuery_Quoted() throws Exception {
        String query = QueryGenerator.getSimpleQuery("\"test quotes\"", false);
        assertTrue("Quoted string not parsed correctly", query.contains("\\\"test quotes\\\""));
        query = QueryGenerator.getSimpleQuery("test quotes", false);
        assertFalse("Text should not have been quoted", query.contains("\\\"test quotes\\\""));
    }
    @Test
    public void testGetSimpleQuery_withoutHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test", false);
        assertFalse(query.contains("highlight"));
    }

    @Test
    public void testGetSimpleQuery_withHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test",true);
        Assert.assertTrue(query.contains("highlight"));
    }
}
