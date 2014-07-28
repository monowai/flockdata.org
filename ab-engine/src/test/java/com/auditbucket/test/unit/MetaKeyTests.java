package com.auditbucket.test.unit;

import com.auditbucket.track.model.MetaKey;
import org.junit.Test;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * User: mike
 * Date: 28/07/14
 * Time: 1:52 PM
 */
public class MetaKeyTests {

    @Test
    public void equalityAndDefaults() throws Exception{

        // ToDo: Case sensitivity
        MetaKey metaKeyA = new MetaKey("abc", "123", "456");
        MetaKey metaKeyB = new MetaKey("abc", "123", "456");

        assertEquals("Keys should match", metaKeyA,metaKeyB);
        assertEquals("Hashcodes should match", metaKeyA.hashCode(), metaKeyB.hashCode());

        MetaKey metaKeyC = new MetaKey("abc", null, "456");
        assertEquals("WildCard document not working", "*", metaKeyC.getDocumentType());
        metaKeyC = new MetaKey();
        assertEquals("WildCard document not working", "*", metaKeyC.getDocumentType());
    }
}
