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
public class AuditBucketResultBeanTest {
    @Test
    public void testGetAuditKeyAuditResultBean() throws Exception {
        TrackResultBean trackResultBean = new TrackResultBean("", "", "", "auditKey");
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(trackResultBean);
        Assert.assertEquals(auditBucketResultBean.getAuditKey(), "auditKey");
    }

    @Test
    public void testGetAuditKeyAuditLogInputBean() throws Exception {
        LogInputBean logInputBean = new LogInputBean("auditKey", "", new DateTime(), null);
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(logInputBean);
        Assert.assertEquals(auditBucketResultBean.getAuditKey(), "auditKey");
    }

    @Test
    public void testGetResultAuditResultBean() throws Exception {
        TrackResultBean trackResultBean = new TrackResultBean("", "", "", "auditKey");
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(trackResultBean);
        Assert.assertEquals(auditBucketResultBean.getResult().getClass(), TrackResultBean.class);
    }

    @Test
    public void testGetResultAuditLogInputBean() throws Exception {
        LogInputBean logInputBean = new LogInputBean("auditKey", "", new DateTime(), null);
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(logInputBean);
        Assert.assertEquals(auditBucketResultBean.getResult().getClass(), LogInputBean.class);
    }
}
