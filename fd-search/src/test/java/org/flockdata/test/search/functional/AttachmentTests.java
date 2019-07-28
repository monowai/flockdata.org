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

import org.flockdata.data.Entity;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.endpoint.FdQueryEP;
import org.flockdata.test.helper.ContentDataHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mholdsworth
 * @since 15/09/2014
 */
public class AttachmentTests extends ESBase {

  @Autowired
  FdQueryEP searchEP;

  //    @Test  DAT-521
  public void attachment_PdfIndexedAndFound() throws Exception {
    // ToDo: FixMe Not working since ES 1.6
    // https://github.com/elastic/elasticsearch-mapper-attachments/issues/20131
//        if ( true==true )
//            return ;
    Entity entity = getEntity("cust", "fort", "anyuser", "fort");

    EntitySearchChange changeA = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
    changeA.setAttachment(ContentDataHelper.getPdfDoc());

    deleteEsIndex(entity);

    indexMappingService.ensureIndexMapping(changeA);
    changeA = entityWriter.handle(changeA);
    Thread.sleep(1000);
    assertNotNull(changeA);
    assertNotNull(changeA.getSearchKey());
    doQuery(entity, "brown");

  }


}
