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

import org.flockdata.model.Entity;
import org.flockdata.search.endpoint.FdQueryEP;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.SearchChange;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/09/14
 * Time: 3:26 PM
 */
public class AttachmentTests extends ESBase {

    @Autowired
    FdQueryEP searchEP;

//    @Test  DAT-521
    public void attachment_PdfIndexedAndFound() throws Exception {
        // ToDo: FixMe Not working since ES 1.6
        // https://github.com/elastic/elasticsearch-mapper-attachments/issues/131
//        if ( true==true )
//            return ;
        Entity entity = getEntity("cust", "fort", "anyuser", "fort");

        SearchChange changeA = new EntitySearchChange(entity, indexHelper.parseIndex(entity));
        changeA.setAttachment(EntityContentHelper.getPdfDoc());

        deleteEsIndex(entity);

        indexMappingService.ensureIndexMapping(changeA);
        changeA = searchRepo.handle(changeA);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());
        doQuery(entity, "brown", 1);

    }



}
