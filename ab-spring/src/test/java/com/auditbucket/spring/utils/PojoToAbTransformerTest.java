package com.auditbucket.spring.utils;

import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.spring.annotations.DatagioCallerRef;
import com.auditbucket.spring.annotations.DatagioUid;
import com.auditbucket.spring.annotations.Trackable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PojoToAbTransformerTest {

    @Test
    public void testTransformPojoPublicField() throws IllegalAccessException, IOException, DatagioException {
        Pojo1 pojo1 = new Pojo1();
        pojo1.email = "email@email.com";
        pojo1.id = 1L;
        pojo1.name = "name";
        MetaInputBean metaInputBean = PojoToAbTransformer.transformToAbFormat(pojo1);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(metaInputBean.getDocumentType(), "pojo1");
        Assert.assertEquals(metaInputBean.getCallerRef(), "email@email.com");
    }

    @Test
    public void testTransformPojoPublicFieldWithCustomDocType() throws IllegalAccessException, IOException, DatagioException {
        Pojo2 pojo2 = new Pojo2();
        pojo2.email = "email@email.com";
        pojo2.id = 1L;
        pojo2.name = "name";
        MetaInputBean metaInputBean = PojoToAbTransformer.transformToAbFormat(pojo2);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(metaInputBean.getDocumentType(), "testDocType");
        Assert.assertEquals(metaInputBean.getCallerRef(), "email@email.com");
    }

    @Test
    public void testTransformPojoPrivateField() throws IllegalAccessException, IOException, DatagioException {
        Pojo3 pojo3 = new Pojo3();
        pojo3.email = "email@email.com";
        pojo3.id = 1L;
        pojo3.name = "name";
        MetaInputBean metaInputBean = PojoToAbTransformer.transformToAbFormat(pojo3);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(metaInputBean.getDocumentType(), "pojo3");
        Assert.assertEquals(metaInputBean.getCallerRef(), "email@email.com");
    }

    @Trackable
    public static class Pojo1 {
        @DatagioUid
        public Long id;
        public String name;
        @DatagioCallerRef
        public String email;
    }

    @Trackable(documentType = "testDocType")
    public static class Pojo2 {
        @DatagioUid
        public Long id;
        public String name;
        @DatagioCallerRef
        public String email;
    }

    @Trackable
    public static class Pojo3 {
        @DatagioUid
        private Long id;
        private String name;
        @DatagioCallerRef
        private String email;
    }
}
