package org.flockdata.test.search.functional;

import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

/**
 * Created by mike on 10/09/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestParentChild extends ESBase {
    @Test
    public void testParentChildStructure () throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "Common";
        String company = "Anyco";
        String parent = "Parent";
        String child = "Child";
        String user = "mike";

        Entity parentEntity = Helper.getEntity(company, fortress, user, parent, "123");
        DocumentType documentType = new DocumentType(parentEntity.getFortress(), parent);
        EntityKeyBean parentKey = new EntityKeyBean(documentType, parentEntity.getCode());

        EntitySearchChange change = new EntitySearchChange(parentEntity);

        trackService.createSearchableChange(new EntitySearchChanges(change));

        // Children have to be in the same company/fortress.
        // ES connects the Child to a Parent. Parents don't need to know about children
        Entity childEntity = Helper.getEntity(company, fortress, user, child);
        EntitySearchChange childChange = new EntitySearchChange(childEntity);
        childChange.setParent(parentKey);

        trackService.createSearchableChange(new EntitySearchChanges(childChange));

    }
}
