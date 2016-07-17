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

package org.flockdata.test.helper;

import junit.framework.TestCase;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.GeoDataBeans;
import org.junit.Test;

/**
 * Created by mike on 4/08/15.
 */
public class TestGeoDataBeans {

    @Test
    public void nullChecks(){
        GeoDataBeans beans = new GeoDataBeans();
        GeoDataBean bean = new GeoDataBean();
        bean.add("type", "code", "name", null, null);
        beans.add("type", bean);

    }

    @Test
    public void description (){
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
    public void stateDesc (){
        GeoDataBeans beans = new GeoDataBeans();
        GeoDataBean bean = new GeoDataBean();
        bean.add("State", "CA", "California",null,null);
        beans.add("State", bean);
        TestCase.assertEquals(null, beans.getDescription());

        bean = new GeoDataBean();
        bean.add("Country", "US", "United States", 23.123, -45.123);
        beans.add("country", bean);

        TestCase.assertEquals("If there are two beans in the set then a description should be computed", "CA, United States", beans.getDescription());

    }
}
