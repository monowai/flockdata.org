/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.search.functional;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Autocomplete type tests
 *
 * Created by mike on 28/05/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestAutoComplete extends ESBase{

    private Logger logger = LoggerFactory.getLogger(TestAutoComplete.class);

    @Test
    public void completion_ShortCodesIgnored() throws Exception {

        String comp = "comp3";
        String fort = "fort3";
        String user = "mikey";
        Map<String, Object> what = Helper.getRandomMap();

        Entity entity = Helper.getEntity(comp, fort, user, fort, "AZERTY");
        deleteEsIndex(entity.getFortress().getIndexName());

        TagInputBean tagInputA = new TagInputBean("1", "AutoComplete").setName("Finding name should not be found as the code is too short");
        TagInputBean tagInputB = new TagInputBean("21", "AutoComplete").setName("Finding code and name indexed");


        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(Helper.getEntityTag(entity, tagInputA, "rlxname"));
        tags.add(Helper.getEntityTag(entity, tagInputB, "rlxname"));

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setWhat(what);
        change.setTags(tags);

        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        logger.info(doQuery(entity.getFortress().getIndexName(), entity.getCallerRef(), 1));

        doCompletionQuery(entity.getFortress().getIndexName(), "find", 2, "Find by tag name failed");
        doCompletionQuery(entity.getFortress().getIndexName(), "21", 1, "Code is 2 chars and should be indexed");
        doCompletionQuery(entity.getFortress().getIndexName(), "1", 0, "Code less than 2 chars should have been ignored");


    }
    @Test
    public void completion_FindTagsByCodeAndDescription() throws Exception {

        String comp = "comp2";
        String fort = "fort2";
        String user = "mikey";
        Map<String, Object> what = Helper.getRandomMap();

        Entity entity = Helper.getEntity(comp, fort, user, fort, "AZERTY");
        deleteEsIndex(entity.getFortress().getIndexName());

        TagInputBean tagInputA = new TagInputBean("tagCode", "AutoComplete", "blah");

        TagInputBean tagInputB = new TagInputBean("myvalue", "AutoComplete", "blah");
        TagInputBean inst = new TagInputBean("Royal Marsden Free Hospital", "Institution", "inst");
        TagInputBean lead = new TagInputBean("Shepherd, JA", "Person", "lead");
        TagInputBean writer = new TagInputBean("Smith, JA", "Person", "lead");
        TagInputBean procedure = new TagInputBean("Surgical Procedures, Minimally Invasive", "Procedure", "involves");
        TagInputBean procedureB = new TagInputBean("Surgical Instruments", "Procedure", "involves");

        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(Helper.getEntityTag(entity, tagInputA, "rlxname"));
        tags.add(Helper.getEntityTag(entity, tagInputB, "rlxname"));
        tags.add(Helper.getEntityTag(entity, inst, "abc"));
        tags.add(Helper.getEntityTag(entity, lead, "lead"));
        tags.add(Helper.getEntityTag(entity, writer, "writer"));
        tags.add(Helper.getEntityTag(entity, procedure, "proc"));
        tags.add(Helper.getEntityTag(entity, procedureB, "proc"));

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setWhat(what);
        change.setTags(tags);

        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        doQuery(entity.getFortress().getIndexName(), entity.getCallerRef(), 1);

        doCompletionQuery(entity.getFortress().getIndexName(), "tag", 1, "Completion failed");
        doCompletionQuery(entity.getFortress().getIndexName(), "tagc", 1, "Completion failed");
        doCompletionQuery(entity.getFortress().getIndexName(), "tagcod", 1, "Completion failed");

        doCompletionQuery(entity.getFortress().getIndexName(), "shep", 1, "Completion failed");

        doCompletionQuery(entity.getFortress().getIndexName(), "myv", 1, "Completion failed");
        // Only supports "start with"
//        doCompletionQuery(entity.getFortress().getIndexName(), "mars", 1, "Completion failed");

    }
}
