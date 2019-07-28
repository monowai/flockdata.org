/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.spring.utils;

import java.io.IOException;
import org.flockdata.helper.FlockException;
import org.flockdata.spring.annotations.FdCallerRef;
import org.flockdata.spring.annotations.FdUid;
import org.flockdata.spring.annotations.Trackable;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Assert;
import org.junit.Test;

public class PojoToFdPayloadTransformerTest {

  @Test
  public void testTransformPojoPublicField() throws IllegalAccessException, IOException, FlockException {
    Pojo1 pojo1 = new Pojo1();
    pojo1.email = "email@email.com";
    pojo1.id = 1L;
    pojo1.name = "name";
    EntityInputBean entityInputBean = PojoToFdTransformer.transformEntity(pojo1);
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
    EntityInputBean entityInputBean = PojoToFdTransformer.transformEntity(pojo2);
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
    EntityInputBean entityInputBean = PojoToFdTransformer.transformEntity(pojo3);
    //Assert.assertEquals(metaInputBean.getKey(), "1");
    Assert.assertEquals(entityInputBean.getDocumentType().getName(), "pojo3");
    Assert.assertEquals(entityInputBean.getCode(), "email@email.com");
  }

  @Trackable
  public static class Pojo1 {
    @FdUid
    public Long id;
    public String name;
    @FdCallerRef
    public String email;
  }

  @Trackable(documentType = "testDocType")
  public static class Pojo2 {
    @FdUid
    public Long id;
    public String name;
    @FdCallerRef
    public String email;
  }

  @Trackable
  public static class Pojo3 {
    @FdUid
    private Long id;
    private String name;
    @FdCallerRef
    private String email;
  }
}
