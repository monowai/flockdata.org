/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import org.flockdata.search.endpoint.ElasticSearchEP;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.SearchChange;
import org.flockdata.track.model.TrackSearchDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/09/14
 * Time: 3:26 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class AttachmentTests extends ESBase {
    @Autowired
    TrackSearchDao searchRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void attachment_PdfIndexedAndFound() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entity = getEntity("cust", "fort", "anyuser");

        SearchChange changeA = new EntitySearchChange(entity, new ContentInputBean(json));
        changeA.setAttachment(Helper.getPdfDoc());

        // FortB will have
        changeA.setDescription("Test Description");

        deleteEsIndex(entity.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());
        changeA = searchRepo.update(changeA);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());
        doQuery(changeA.getIndexName(), "brown", 1);

    }



}
