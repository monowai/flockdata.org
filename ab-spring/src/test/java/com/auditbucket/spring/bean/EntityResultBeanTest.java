package com.auditbucket.spring.bean;

import com.auditbucket.track.bean.ContentInputBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 05/09/13
 * Time: 23:25
 * To change this template use File | Settings | File Templates.
 */
public class EntityResultBeanTest {

    @Test
    public void testGetEntityLogInputBean() throws Exception {
        ContentInputBean contentInputBean = new ContentInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(contentInputBean);
        Assert.assertEquals(entityResultBean.getMetaKey(), "auditKey");
    }

    @Test
    public void testGetResultLogInputBean() throws Exception {
        ContentInputBean contentInputBean = new ContentInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(contentInputBean);
        Assert.assertEquals(entityResultBean.getResult().getClass(), ContentInputBean.class);
    }
}
