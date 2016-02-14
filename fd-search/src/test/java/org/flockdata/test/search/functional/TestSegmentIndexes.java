package org.flockdata.test.search.functional;

import junit.framework.TestCase;
import org.flockdata.model.Entity;
import org.flockdata.model.FortressSegment;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Search tests related to data stored in data segments
 * Created by mike on 23/10/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestSegmentIndexes extends ESBase{
    /**
     * Fortress are linked to segments that in turn have entities.
     * This approach let's us break up the ElasticSearch indexes in to
     * segmentation boundaries such as 2015-Jan, 2014, "anytext" that is meaningful
     * to the usecase. If you don't supply the data with a segment then the default of "" is used.
     *
     * Consider segmenting annual datasets and transactions. Segmenting master data (i.e. Customer) doesn't really
     * make any sense unless you want to break things up by Branch for instance.
     * @throws Exception
     */
    @Test
    public void test_segementedIndexes() throws Exception {

        String fortress = "Common";
        String company = "company";
        String user = "mike";

//        deleteEsIndex("fd." + company.toLowerCase() + "." + fortress.toLowerCase(), "child");

        Entity entity = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entity);
        entity.setSegment( new FortressSegment(entity.getFortress(), "2014"));
        TestCase.assertEquals("2014", entity.getSegment().getCode());

        EntitySearchChange change = new EntitySearchChange(entity, indexHelper.parseIndex(entity));
        deleteEsIndex(indexHelper.parseIndex(entity));

        esSearchWriter.createSearchableChange(new EntitySearchChanges(change));

        // Each entity will be written to it's own segment
        Entity entityOtherSegment = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entityOtherSegment);
        entityOtherSegment.setSegment( new FortressSegment(entity.getFortress(), "2015"));
        TestCase.assertEquals("2015", entityOtherSegment.getSegment().getCode());

        change = new EntitySearchChange(entityOtherSegment, indexHelper.parseIndex(entityOtherSegment));
        deleteEsIndex(indexHelper.parseIndex(entityOtherSegment));

        esSearchWriter.createSearchableChange(new EntitySearchChanges(change));

        Thread.sleep(2000);
        //"Each doc should be in it's own segmented index"
        doQuery(entity, entity.getMetaKey(), 1);
        //"Each doc should be in it's own segmented index"
        doQuery(entityOtherSegment, entityOtherSegment.getMetaKey(), 1);
        // Scanning across segmented indexes
        String index;
        if (indexHelper.isSuffixed())
            index = entity.getSegment().getFortress().getRootIndex()+".invoice.*";
        else
            index = entity.getSegment().getFortress().getRootIndex()+".*";
        doQuery(index, "invoice", "*", 2);

    }

    // ToDo: getting a mapping when a segment is involved.
    // Should firstly located by the segmented name and then by the index root before falling back
    // to "default". This will give a chance to override an index structure on a per segment basis


}
