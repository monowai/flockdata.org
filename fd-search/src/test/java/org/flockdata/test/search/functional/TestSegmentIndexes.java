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

package org.flockdata.test.search.functional;

import junit.framework.TestCase;
import org.flockdata.model.Entity;
import org.flockdata.model.FortressSegment;
import org.flockdata.search.FdSearch;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchChanges;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Search tests related to data stored in data segments
 * Created by mike on 23/10/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
public class TestSegmentIndexes extends ESBase {
    /**
     * Fortress are linked to segments that in turn have entities.
     * This approach let's us break up the ElasticSearch indexes in to
     * segmentation boundaries such as 2015-Jan, 2014, "anytext" that is meaningful
     * to the usecase. If you don't supply the data with a segment then the default of "" is used.
     * <p>
     * Consider segmenting annual datasets and transactions. Segmenting master data (i.e. Customer) doesn't really
     * make any sense unless you want to break things up by Branch for instance.
     *
     * @throws Exception
     */
    @Test
    public void test_segementedIndexes() throws Exception {

        String fortress = "Common";
        String company = "company";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entity);
        entity.setSegment(new FortressSegment(entity.getFortress(), "2014"));
        TestCase.assertEquals("2014", entity.getSegment().getCode());

        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        deleteEsIndex(indexManager.parseIndex(entity));

        esSearchWriter.createSearchableChange(new SearchChanges(change));

        // Each entity will be written to it's own segment
        Entity entityOtherSegment = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entityOtherSegment);
        entityOtherSegment.setSegment(new FortressSegment(entity.getFortress(), "2015"));
        TestCase.assertEquals("2015", entityOtherSegment.getSegment().getCode());

        change = new EntitySearchChange(entityOtherSegment, indexManager.parseIndex(entityOtherSegment));
        deleteEsIndex(indexManager.parseIndex(entityOtherSegment));

        esSearchWriter.createSearchableChange(new SearchChanges(change));

        Thread.sleep(2000);
        //"Each doc should be in it's own segmented index"
        doQuery(entity, entity.getKey(), 1);
        //"Each doc should be in it's own segmented index"
        doQuery(entityOtherSegment, entityOtherSegment.getKey(), 1);
        // Scanning across segmented indexes
        String index;
        if (indexManager.isSuffixed())
            index = entity.getSegment().getFortress().getRootIndex() + ".invoice.*";
        else
            index = entity.getSegment().getFortress().getRootIndex() + ".*";
        doQuery(index, "invoice", "*", 2);

    }

    // ToDo: getting a mapping when a segment is involved.
    // Should firstly located by the segmented name and then by the index root before falling back
    // to "default". This will give a chance to override an index structure on a per segment basis

    @Test
    public void test_deleteSingleSegment() throws Exception {

        String fortress = "DelSingleSegment";
        String company = "segCompany";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entity);
        entity.setSegment(new FortressSegment(entity.getFortress(), "2014"));
        TestCase.assertEquals("2014", entity.getSegment().getCode());

        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        deleteEsIndex(indexManager.parseIndex(entity));

        esSearchWriter.createSearchableChange(new SearchChanges(change));

        // Each entity will be written to it's own segment
        Entity entityOtherSegment = getEntity(company, fortress, user, "Invoice", "123");
        deleteEsIndex(entityOtherSegment);
        entityOtherSegment.setSegment(new FortressSegment(entity.getFortress(), "2015"));
        TestCase.assertEquals("2015", entityOtherSegment.getSegment().getCode());

        change = new EntitySearchChange(entityOtherSegment, indexManager.parseIndex(entityOtherSegment));
        deleteEsIndex(indexManager.parseIndex(entityOtherSegment));

        esSearchWriter.createSearchableChange(new SearchChanges(change));

        Thread.sleep(2000);
        //"Each doc should be in it's own segmented index"
        doQuery(entity, entity.getKey(), 1);
        //"Each doc should be in it's own segmented index"
        doQuery(entityOtherSegment, entityOtherSegment.getKey(), 1);
        // Scanning across segmented indexes
        String indexToKeep;
        indexToKeep = entity.getSegment().getFortress().getRootIndex() + ".invoice.2014";
        doQuery(indexToKeep, "invoice", "*", 1);

        String indexToDelete;
        indexToDelete = entity.getSegment().getFortress().getRootIndex() + ".invoice.2015";
        doQuery(indexToDelete, "invoice", "*", 1);


        Collection<String> indexes = new ArrayList<>();
        indexes.add(indexToDelete);
        searchAdmin.deleteIndexes(indexes);
        Thread.sleep(500);
        // 2015 should remain
        doQuery(indexToKeep, "invoice", "*", 1);
        // 2014 is removed
        doQuery(indexToDelete, "invoice", "*", 0);

    }

}
