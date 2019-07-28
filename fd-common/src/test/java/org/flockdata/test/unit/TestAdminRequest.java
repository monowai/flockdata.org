/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flockdata.helper.JsonUtils;
import org.flockdata.search.AdminRequest;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 12/05/2016
 */
public class TestAdminRequest {

  @Test
  public void jsonSerialization() throws Exception {

    AdminRequest adminRequest = new AdminRequest("delete.this.index");

    assertEquals(1, adminRequest.getIndexesToDelete().size());
    String json = JsonUtils.toJson(adminRequest);
    assertNotNull(json);
    AdminRequest deserialized = JsonUtils.toObject(json.getBytes(), AdminRequest.class);
    assertNotNull(deserialized.getIndexesToDelete());
    assertEquals(1, deserialized.getIndexesToDelete().size());
    assertEquals(adminRequest.getIndexesToDelete().iterator().next(), deserialized.getIndexesToDelete().iterator().next());

  }
}
