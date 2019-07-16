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

import java.util.HashMap;
import java.util.Map;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.bean.StorageBean;
import org.junit.Test;

/**
 * @author mike
 * @tag
 * @since 28/02/17
 */
public class TestStorageBeans {

    @Test
    public void storedContent() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "property");
        StorageBean storedContent = new StorageBean(123, data);
        storedContent.setStore("SomeStore");
        byte[] bytes = JsonUtils.toJsonBytes(storedContent);
        assertNotNull(JsonUtils.toObject(bytes, StorageBean.class));
    }
}
