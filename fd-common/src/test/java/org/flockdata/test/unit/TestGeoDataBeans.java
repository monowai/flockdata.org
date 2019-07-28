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

import static junit.framework.TestCase.assertNotNull;

import junit.framework.TestCase;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.GeoDataBeans;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 4/08/2015
 */
public class TestGeoDataBeans {

  @Test
  public void nullChecks() {
    GeoDataBeans beans = new GeoDataBeans();
    GeoDataBean bean = new GeoDataBean();
    bean.add("type", "code", "name", null, null);
    beans.add("type", bean);

  }

  @Test
  public void zeroChecks() {
    Integer zero = 0;
    Double answer = Double.parseDouble(zero.toString());
    assertNotNull(answer);

  }


  @Test
  public void description() {
    GeoDataBeans beans = new GeoDataBeans();
    GeoDataBean bean = new GeoDataBean();
    bean.add("type", "code", "name", 23.123, -45.123);
    beans.add("type", bean);
    TestCase.assertEquals(null, beans.getDescription());

    bean = new GeoDataBean();
    bean.add("type2", "xxx", "name", 23.123, -45.123);
    beans.add("type2", bean);

    TestCase.assertEquals("If there are two beans in the set then a description should be computed", "name, name", beans.getDescription());

  }

  @Test
  public void stateDesc() {
    GeoDataBeans beans = new GeoDataBeans();
    GeoDataBean bean = new GeoDataBean();
    bean.add("State", "CA", "California", null, null);
    beans.add("State", bean);
    TestCase.assertEquals(null, beans.getDescription());

    bean = new GeoDataBean();
    bean.add("Country", "US", "United States", 23.123, -45.123);
    beans.add("country", bean);

    TestCase.assertEquals("If there are two beans in the set then a description should be computed", "CA, United States", beans.getDescription());

  }
}
