package com.auditbucket.test.unit;

import com.auditbucket.track.model.EntityKey;
import org.junit.Test;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * User: mike
 * Date: 28/07/14
 * Time: 1:52 PM
 */
public class EntityKeyTests {

    @Test
    public void equalityAndDefaults() throws Exception{

        // ToDo: Case sensitivity
        EntityKey entityKeyA = new EntityKey("abc", "123", "456");
        EntityKey entityKeyB = new EntityKey("abc", "123", "456");

        assertEquals("Keys should match", entityKeyA, entityKeyB);
        assertEquals("Hashcodes should match", entityKeyA.hashCode(), entityKeyB.hashCode());

        EntityKey entityKeyC = new EntityKey("abc", null, "456");
        assertEquals("WildCard document not working", "*", entityKeyC.getDocumentType());
        entityKeyC = new EntityKey();
        assertEquals("WildCard document not working", "*", entityKeyC.getDocumentType());
    }
}
