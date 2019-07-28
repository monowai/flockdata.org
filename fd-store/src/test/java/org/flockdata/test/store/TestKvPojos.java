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

package org.flockdata.test.store;

import static junit.framework.TestCase.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 19/01/2016
 */
public class TestKvPojos {

  @Test
  public void jsonSerialization() throws Exception {
    String fortress = "Entity Test";
    String docType = "TestAuditX";
    String entityCode = "ABC123R";
    String company = "company";

    Map<String, Object> what = ContentDataHelper.getRandomMap();
    Fortress fort = MockDataFactory.getFortress("test", MockDataFactory.getCompany("MyName"));
    when(fort.isSearchEnabled()).thenReturn(false);

    // Represents identifiable entity information
    EntityInputBean entityInputBean = new EntityInputBean(fort, "wally", docType, new DateTime(), entityCode)
        .setContent(new ContentInputBean(what));

    Document documentType = MockDataFactory.getDocument(fort, docType);
    // The "What" content

    // Emulate the creation of the entity
    Entity entity = MockDataFactory.getEntity(company, fortress, "wally", documentType.getName(), null);

    // Wrap the entity in a Track Result
    // TrackResultBean represents the general accumulated payload
    TrackResultBean trackResultBean = new TrackResultBean(fort, entity, documentType, entityInputBean);
    StorageBean storeBean = new StorageBean(trackResultBean);

    byte[] bytes = JsonUtils.toJsonBytes(storeBean);
    StorageBean deserializedBean = JsonUtils.toObject(bytes, StorageBean.class);
    TestCase.assertNotNull(deserializedBean);
    assertNull("Not handling a null fortress name ", Fortress.code(null));
  }
}
