/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.test.batch;

import org.flockdata.shared.FdBatcher;
import org.flockdata.test.client.AbstractImport;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 6/03/16.
 */
public class TestFdBatcher extends AbstractImport{

    @Autowired
    FdBatcher myBatcher;
    @Test
    public void fdBatcherAccumulation() throws Exception {
        EntityInputBean eib = new EntityInputBean("fort", "type");
        eib.setCode("tt111");

        myBatcher.batchEntity(eib);
        myBatcher.batchEntity(eib);
        assertEquals(1, myBatcher.getEntities().size());

        eib = new EntityInputBean("fort", "Type");
        eib.setCode("tt222");
        myBatcher.batchEntity(eib);
        myBatcher.batchEntity(eib);

        assertEquals(2, myBatcher.getEntities().size());
    }
}
