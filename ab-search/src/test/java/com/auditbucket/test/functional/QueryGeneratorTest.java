package com.auditbucket.test.functional;

import com.auditbucket.search.helper.QueryGenerator;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: macpro
 * Date: 04/08/2014
 * Time: 15:31
 * To change this template use File | Settings | File Templates.
 */
public class QueryGeneratorTest {
    @Test
    public void testGetSimpleQuery_withoutHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test", false);
        Assert.assertEquals("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"should\": [\n" +
                "        {\n" +
                "          \"query_string\": {\n" +
                "            \"query\": \"test\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}", query);
    }

    @Test
    public void testGetSimpleQuery_withHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test",true);
        Assert.assertEquals("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"should\": [\n" +
                "        {\n" +
                "          \"query_string\": {\n" +
                "            \"query\": \"test\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"highlight\": {\n" +
                "    \"fields\": {\n" +
                "      \"*\": {}\n" +
                "    }\n" +
                "  }\n" +
                "}", query);
    }
}
