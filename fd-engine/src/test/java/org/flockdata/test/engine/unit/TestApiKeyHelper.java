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

import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;

/**
 * @author mholdsworth
 * @since 28/07/2014
 * @tag Test, APIKey

 */
public class TestApiKeyHelper {
    @org.junit.Test
    @Deprecated
    public void testApiKeyOrder(){
        String key = "httpHeader";
        String requestKey = "httpRequest";
        String nullReturned = "{{hello}}";
        String headerReturned = "{{hello";

        Assert.assertEquals("HttpHeader key take precedence over request key", key, ApiKeyInterceptor.ApiKeyHelper.resolveKey(key, requestKey));
        assertEquals(requestKey, ApiKeyInterceptor.ApiKeyHelper.resolveKey(null, requestKey));
        assertEquals("HttpHeader key starting in {{ and ending in }} are ignored - POSTMan testing", null, ApiKeyInterceptor.ApiKeyHelper.resolveKey(nullReturned, null));
        assertEquals(requestKey, ApiKeyInterceptor.ApiKeyHelper.resolveKey(nullReturned, requestKey));
        assertEquals("HttpHeader key is valid as it doesn't end in }}", headerReturned, ApiKeyInterceptor.ApiKeyHelper.resolveKey(headerReturned, null));
    }
}
