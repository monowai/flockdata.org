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

package org.flockdata.test.unit;

import static junit.framework.TestCase.assertNotNull;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 23/10/2015
 */
public class TestEntityKeyBean {

    @Test
    public void nullCompanyIssues() throws Exception {
        EntityKeyBean ekb = new EntityKeyBean("docTypeName", "fortress", "code");
        String json = JsonUtils.toJson(ekb);
        assertNotNull(json);
        EntityKeyBean otherBean = JsonUtils.toObject(json.getBytes(), EntityKeyBean.class);
        TestCase.assertEquals(ekb.getCode(), otherBean.getCode());
        TestCase.assertEquals(ekb.getFortressName(), otherBean.getFortressName());
        TestCase.assertEquals(ekb.getDocumentType(), otherBean.getDocumentType());
    }
}
