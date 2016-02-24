/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.spring.utils;

import org.flockdata.helper.FlockException;
import org.flockdata.spring.annotations.DatagioCallerRef;
import org.flockdata.spring.annotations.DatagioUid;
import org.flockdata.spring.annotations.Trackable;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PojoToFdTransformerTest {

    @Test
    public void testTransformPojoPublicField() throws IllegalAccessException, IOException, FlockException {
        Pojo1 pojo1 = new Pojo1();
        pojo1.email = "email@email.com";
        pojo1.id = 1L;
        pojo1.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo1);
        //Assert.assertEquals(metaInputBean.getKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType().getName(), "pojo1");
        Assert.assertEquals(entityInputBean.getCode(), "email@email.com");
    }

    @Test
    public void testTransformPojoPublicFieldWithCustomDocType() throws IllegalAccessException, IOException, FlockException {
        Pojo2 pojo2 = new Pojo2();
        pojo2.email = "email@email.com";
        pojo2.id = 1L;
        pojo2.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo2);
        //Assert.assertEquals(metaInputBean.getKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType().getName(), "testDocType");
        Assert.assertEquals(entityInputBean.getCode(), "email@email.com");
    }

    @Test
    public void testTransformPojoPrivateField() throws IllegalAccessException, IOException, FlockException {
        Pojo3 pojo3 = new Pojo3();
        pojo3.email = "email@email.com";
        pojo3.id = 1L;
        pojo3.name = "name";
        EntityInputBean entityInputBean = PojoToAbTransformer.transformToAbFormat(pojo3);
        //Assert.assertEquals(metaInputBean.getKey(), "1");
        Assert.assertEquals(entityInputBean.getDocumentType().getName(), "pojo3");
        Assert.assertEquals(entityInputBean.getCode(), "email@email.com");
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
