package com.auditbucket.spring.bean;

import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.bean.AuditResultBean;
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
        AuditResultBean auditResultBean = new AuditResultBean("", "", "", "auditKey");
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(auditResultBean);
        Assert.assertEquals(auditBucketResultBean.getAuditKey(), "auditKey");
    }

    @Test
    public void testGetAuditKeyAuditLogInputBean() throws Exception {
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean("auditKey", "", new DateTime(), null);
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(auditLogInputBean);
        Assert.assertEquals(auditBucketResultBean.getAuditKey(), "auditKey");
    }

    @Test
    public void testGetResultAuditResultBean() throws Exception {
        AuditResultBean auditResultBean = new AuditResultBean("", "", "", "auditKey");
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(auditResultBean);
        Assert.assertEquals(auditBucketResultBean.getResult().getClass(), AuditResultBean.class);
    }

    @Test
    public void testGetResultAuditLogInputBean() throws Exception {
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean("auditKey", "", new DateTime(), null);
        AuditBucketResultBean auditBucketResultBean = new AuditBucketResultBean(auditLogInputBean);
        Assert.assertEquals(auditBucketResultBean.getResult().getClass(), AuditLogInputBean.class);
    }
}
