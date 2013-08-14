package com.auditbucket.spring.utils;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.spring.annotations.AuditClientRef;
import com.auditbucket.spring.annotations.AuditKey;
import com.auditbucket.spring.annotations.Auditable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 14/08/13
 * Time: 12:22
 * To change this template use File | Settings | File Templates.
 */
public class PojoToAbTransformerTest {

    @Test
    public void testTransformPojoPublicField() throws IllegalAccessException {
        Pojo1 pojo1 = new Pojo1();
        pojo1.email="email@email.com";
        pojo1.id=1L;
        pojo1.name="name";
        AuditHeaderInputBean auditHeaderInputBean = PojoToAbTransformer.transformToAbFormat(pojo1) ;
        Assert.assertEquals(auditHeaderInputBean.getAuditKey(),"1");
        Assert.assertEquals(auditHeaderInputBean.getDocumentType(),"pojo1");
        Assert.assertEquals(auditHeaderInputBean.getCallerRef(),"email@email.com");
    }

    @Test
    public void testTransformPojoPublicFieldWithCustomDocType() throws IllegalAccessException {
        Pojo2 pojo2 = new Pojo2();
        pojo2.email="email@email.com";
        pojo2.id=1L;
        pojo2.name="name";
        AuditHeaderInputBean auditHeaderInputBean = PojoToAbTransformer.transformToAbFormat(pojo2) ;
        Assert.assertEquals(auditHeaderInputBean.getAuditKey(),"1");
        Assert.assertEquals(auditHeaderInputBean.getDocumentType(),"testDocType");
        Assert.assertEquals(auditHeaderInputBean.getCallerRef(),"email@email.com");
    }

    @Test
    public void testTransformPojoPrivateField() throws IllegalAccessException {
        Pojo3 pojo3 = new Pojo3();
        pojo3.email="email@email.com";
        pojo3.id=1L;
        pojo3.name="name";
        AuditHeaderInputBean auditHeaderInputBean = PojoToAbTransformer.transformToAbFormat(pojo3) ;
        Assert.assertEquals(auditHeaderInputBean.getAuditKey(),"1");
        Assert.assertEquals(auditHeaderInputBean.getDocumentType(),"pojo3");
        Assert.assertEquals(auditHeaderInputBean.getCallerRef(),"email@email.com");
    }

    @Auditable
    public static class Pojo1 {
        @AuditKey
        public Long id;

        public String name;

        @AuditClientRef
        public String email;
    }

    @Auditable(documentType="testDocType")
    public static class Pojo2 {
        @AuditKey
        public Long id;

        public String name;

        @AuditClientRef
        public String email;
    }

    @Auditable
    public static class Pojo3 {
        @AuditKey
        private Long id;

        private String name;

        @AuditClientRef
        private String email;
    }
}
