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
