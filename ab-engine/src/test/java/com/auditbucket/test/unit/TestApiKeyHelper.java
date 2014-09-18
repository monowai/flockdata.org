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
    @Deprecated
    public void testApiKeyOrder(){
        String metaKey = "httpHeader";
        String requestKey = "httpRequest";
        String nullReturned = "{{hello}}";
        String headerReturned = "{{hello";

        assertEquals("HttpHeader key take precedence over request key", metaKey, ApiKeyHelper.resolveKey(metaKey, requestKey));
        assertEquals(requestKey, ApiKeyHelper.resolveKey(null, requestKey));
        assertEquals("HttpHeader key starting in {{ and ending in }} are ignored - POSTMan testing", null, ApiKeyHelper.resolveKey(nullReturned, null));
        assertEquals(requestKey, ApiKeyHelper.resolveKey(nullReturned, requestKey));
        assertEquals("HttpHeader key is valid as it doesn't end in }}", headerReturned, ApiKeyHelper.resolveKey(headerReturned, null));
    }
}
