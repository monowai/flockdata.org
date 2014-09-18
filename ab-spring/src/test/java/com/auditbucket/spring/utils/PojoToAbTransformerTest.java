package com.auditbucket.spring.utils;

import com.auditbucket.helper.FlockException;
import com.auditbucket.spring.annotations.DatagioCallerRef;
import com.auditbucket.spring.annotations.DatagioUid;
import com.auditbucket.spring.annotations.Trackable;
import com.auditbucket.track.bean.EntityInputBean;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PojoToAbTransformerTest {

    @Test
    public void testTransformPojoPublicField() throws IllegalAccessException, IOException, FlockException {
        Pojo1 pojo1 = new Pojo1();
        pojo1.email = "email@email.com";
        pojo1.id = 1L;
        pojo1.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo1);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType(), "pojo1");
        Assert.assertEquals(entityInputBean.getCallerRef(), "email@email.com");
    }

    @Test
    public void testTransformPojoPublicFieldWithCustomDocType() throws IllegalAccessException, IOException, FlockException {
        Pojo2 pojo2 = new Pojo2();
        pojo2.email = "email@email.com";
        pojo2.id = 1L;
        pojo2.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo2);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType(), "testDocType");
        Assert.assertEquals(entityInputBean.getCallerRef(), "email@email.com");
    }

    @Test
    public void testTransformPojoPrivateField() throws IllegalAccessException, IOException, FlockException {
        Pojo3 pojo3 = new Pojo3();
        pojo3.email = "email@email.com";
        pojo3.id = 1L;
        pojo3.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo3);
        //Assert.assertEquals(metaInputBean.getMetaKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType(), "pojo3");
        Assert.assertEquals(entityInputBean.getCallerRef(), "email@email.com");
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
