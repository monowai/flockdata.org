/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Autocomplete type tests
 *
 * @author mholdsworth
 * @since 28/05/2015
 */
@RunWith(SpringRunner.class)
public class TestAutoComplete extends ESBase {

    private Logger logger = LoggerFactory.getLogger(TestAutoComplete.class);

    @Test
    public void completion_numericCodesIgnored() throws Exception {
        String comp = "comp4";
        String fort = "fort4";
        String user = "mikey";
        Map<String, Object> what = ContentDataHelper.getRandomMap();

        Entity entity = getEntity(comp, fort, user, fort, "AZERTY");
        deleteEsIndex(entity);

        // 2 char code as this is the minimum we will index from
        TagInputBean noName = new TagInputBean("11", "NumCode", "rlxname");
        TagInputBean numCodeWithName = new TagInputBean("21", "AutoComplete", "rlxname")
            .setName("Code should not be indexed");
        TagInputBean zipCode = new TagInputBean("70612", "ZipCode");

        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(MockDataFactory.getEntityTag(entity, noName, "rlxname"));
        tags.add(MockDataFactory.getEntityTag(entity, numCodeWithName, "rlxname"));
        tags.add(MockDataFactory.getEntityTag(entity, zipCode, "zip"));

        EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
        change.setData(what);
        change.setStructuredTags(EntityTag.TAG_STRUCTURE.DEFAULT, tags);

        indexMappingService.ensureIndexMapping(change);
        SearchChange searchResult = entityWriter.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        logger.info(doQuery(entity, entity.getCode()));

        doCompletionQuery(entity, noName.getCode(), 1, "Should be found as there is no name");
        doCompletionQuery(entity, "code", 1, "Find by name, but Code is not indexed");
        doCompletionQuery(entity, numCodeWithName.getCode(), 0, "Should not be found as numeric code is ignored");
        doCompletionQuery(entity, zipCode.getCode(), 1, "Didn't find the zip code");
        doTermQuery(entity, "tag.rlxname.autocomplete.code", numCodeWithName.getCode(), 0, "AutoComplete code should not be indexed");
        doTermQuery(entity, "tag.rlxname.autocomplete.name", numCodeWithName.getName());


    }

    @Test
    public void completion_ShortCodesIgnored() throws Exception {

        String comp = "comp3";
        String fort = "fort3";
        String user = "mikey";
        Map<String, Object> what = ContentDataHelper.getRandomMap();

        Entity entity = getEntity(comp, fort, user, fort, "AZERTY");
        deleteEsIndex(entity);

        TagInputBean tagInputA = new TagInputBean("A", "AutoComplete").setName("Finding name should not be found as the code is too short");
        TagInputBean tagInputB = new TagInputBean("AB", "AutoComplete").setName("Finding code and name indexed");


        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(MockDataFactory.getEntityTag(entity, tagInputA, "rlxname"));
        tags.add(MockDataFactory.getEntityTag(entity, tagInputB, "rlxname"));

        EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
        change.setData(what);
        change.setStructuredTags(EntityTag.TAG_STRUCTURE.DEFAULT, tags);

        indexMappingService.ensureIndexMapping(change);
        SearchChange searchResult = entityWriter.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        logger.info(doQuery(entity, entity.getCode()));

        doCompletionQuery(entity, "find", 1, "Find by tag name failed");
        doCompletionQuery(entity, "ab", 1, "Code is 2 chars and should be indexed");
        doCompletionQuery(entity, "a", 0, "Code less than 2 chars should have been ignored");
    }

    @Test
    public void completion_FindTagsByCodeAndDescription() throws Exception {
        String comp = "comp2";
        String fort = "fort2";
        String user = "mikey";
        Map<String, Object> what = ContentDataHelper.getRandomMap();

        Entity entity = getEntity(comp, fort, user, fort, "AZERTY");
//        deleteEsIndex(entity);

        TagInputBean tagInputA = new TagInputBean("tagCode", "AutoComplete", "blah");

        TagInputBean tagInputB = new TagInputBean("myvalue", "AutoComplete", "blah");
        TagInputBean inst = new TagInputBean("Royal Marsden Free Hospital", "Institution", "inst");
        TagInputBean lead = new TagInputBean("Shepherd, JA", "Person", "lead");
        TagInputBean writer = new TagInputBean("Smith, JA", "Person", "lead");
        TagInputBean procedure = new TagInputBean("Surgical Procedures, Minimally Invasive", "Procedure", "involves");
        TagInputBean procedureB = new TagInputBean("Surgical Instruments", "Procedure", "involves");

        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(MockDataFactory.getEntityTag(entity, tagInputA, "rlxname"));
        tags.add(MockDataFactory.getEntityTag(entity, tagInputB, "rlxname"));
        tags.add(MockDataFactory.getEntityTag(entity, inst, "abc"));
        tags.add(MockDataFactory.getEntityTag(entity, lead, "lead"));
        tags.add(MockDataFactory.getEntityTag(entity, writer, "writer"));
        tags.add(MockDataFactory.getEntityTag(entity, procedure, "proc"));
        tags.add(MockDataFactory.getEntityTag(entity, procedureB, "proc"));

        EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
        change.setData(what);
        change.setStructuredTags(EntityTag.TAG_STRUCTURE.DEFAULT, tags);

        indexMappingService.ensureIndexMapping(change);
        SearchChange searchResult = entityWriter.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        doQuery(entity, entity.getCode());

        doCompletionQuery(entity, "tag", 1, "Completion failed");
        doCompletionQuery(entity, "tagc", 1, "Completion failed");
        doCompletionQuery(entity, "tagcod", 1, "Completion failed");

        doCompletionQuery(entity, "shep", 1, "Completion failed");

        doCompletionQuery(entity, "myv", 1, "Completion failed");
        // Only supports "start with"
//        doCompletionQuery(entity.getFortress().getIndexName(), "mars", 1, "Completion failed");

    }
}
