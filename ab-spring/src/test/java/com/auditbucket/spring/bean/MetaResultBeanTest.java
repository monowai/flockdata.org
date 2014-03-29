package com.auditbucket.spring.bean;

import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.TrackResultBean;
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
public class MetaResultBeanTest {
    @Test
    public void testGetAuditKeyAuditResultBean() throws Exception {
        TrackResultBean trackResultBean = new TrackResultBean("", "", "", "auditKey");
        MetaResultBean metaResultBean = new MetaResultBean(trackResultBean);
        Assert.assertEquals(metaResultBean.getMetaKey(), "auditKey");
    }

    @Test
    public void testGetAuditKeyAuditLogInputBean() throws Exception {
        LogInputBean logInputBean = new LogInputBean("auditKey", "", new DateTime(), null);
        MetaResultBean metaResultBean = new MetaResultBean(logInputBean);
        Assert.assertEquals(metaResultBean.getMetaKey(), "auditKey");
    }

    @Test
    public void testGetResultAuditResultBean() throws Exception {
        TrackResultBean trackResultBean = new TrackResultBean("", "", "", "auditKey");
        MetaResultBean metaResultBean = new MetaResultBean(trackResultBean);
        Assert.assertEquals(metaResultBean.getResult().getClass(), TrackResultBean.class);
    }

    @Test
    public void testGetResultAuditLogInputBean() throws Exception {
        LogInputBean logInputBean = new LogInputBean("auditKey", "", new DateTime(), null);
        MetaResultBean metaResultBean = new MetaResultBean(logInputBean);
        Assert.assertEquals(metaResultBean.getResult().getClass(), LogInputBean.class);
    }
}
