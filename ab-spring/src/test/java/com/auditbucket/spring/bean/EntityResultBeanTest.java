package com.auditbucket.spring.bean;

import com.auditbucket.track.bean.LogInputBean;
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
        LogInputBean logInputBean = new LogInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(logInputBean);
        Assert.assertEquals(entityResultBean.getMetaKey(), "auditKey");
    }

    @Test
    public void testGetResultLogInputBean() throws Exception {
        LogInputBean logInputBean = new LogInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(logInputBean);
        Assert.assertEquals(entityResultBean.getResult().getClass(), LogInputBean.class);
    }
}
