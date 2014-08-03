package com.auditbucket.test.unit;

import com.auditbucket.helper.ApiKeyHelper;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 28/07/14
 * Time: 11:44 AM
 */
public class TestApiKeyHelper {
    @org.junit.Test
    public void testApiKeyOrder(){
        String headerKey = "header";
        String requestKey = "request";
        String nullReturned = "{{hello}}";
        String headerReturned = "{{hello";

        assertEquals("Header key take presedence over request key", headerKey, ApiKeyHelper.resolveKey(headerKey, requestKey));
        assertEquals(requestKey, ApiKeyHelper.resolveKey(null, requestKey));
        assertEquals("Header keys starting in {{ and ending in }} are ignored - POSTMan testing", null, ApiKeyHelper.resolveKey(nullReturned, null));
        assertEquals(requestKey, ApiKeyHelper.resolveKey(nullReturned, requestKey));
        assertEquals("Header is valid as it doesn't end in }}", headerReturned, ApiKeyHelper.resolveKey(headerReturned, null));
    }
}
