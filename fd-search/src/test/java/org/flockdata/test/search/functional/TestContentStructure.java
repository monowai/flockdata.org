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

import org.flockdata.model.*;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.FdSearch;
import org.flockdata.search.model.*;
import org.flockdata.test.helper.EntityContentHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 31/08/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
public class TestContentStructure extends ESBase {

    @Test
    public void contentFieldsReturned() throws Exception{
        Map<String, Object> json = EntityContentHelper.getBigJsonText(2);

        String fortress = "contentFieldsReturned";
        String company = "company";
        String doc = "doc";
        String user = "mike";

        json.put("numeric", 100);

        Entity entity = getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));

        Collection<EntityTag> tags = new ArrayList<>();
        EntityTag tag = new EntityTagOut(entity, new Tag(new TagInputBean("SomeCode", "SomeLabel", new EntityTagRelationshipInput("blah"))));
        tag.getTag().addProperty("mynum", 100);
        tags.add(tag);
        change.setStructuredTags(tags);
        change.setDescription("Test Description");
        change.setData(json);

        esSearchWriter.createSearchableChange(new SearchChanges(change));
        Thread.sleep(600);
        QueryParams queryParams = new QueryParams();
        queryParams.setCompany(company);
        queryParams.setTypes(doc);
        queryParams.setFortress(fortress);

        ContentStructure dataStructure = contentService.getStructure(queryParams);
        assertNotNull ( dataStructure);
        Collection<EsColumn> dataFields = dataStructure.getData();
        assertEquals("Un-faceted string fields should not be returned",1, dataFields.size());
        assertEquals("Expected a tag and its numeric user defined property", 2, dataStructure.getLinks().size());
        Collection<EsColumn> linkFields = dataStructure.getLinks();
        for (EsColumn linkField : linkFields) {
            if ( linkField.getName().endsWith(".facet"))
                assertEquals ("entity-tag-out.somelabel.code", linkField.getDisplayName());
            else
                assertEquals ("entity-tag-out.somelabel.mynum", linkField.getDisplayName());
        }

        Collection<EsColumn>fdFields = dataStructure.getSystem();
        assertEquals(3, fdFields.size());
    }


}
