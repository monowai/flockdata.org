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

package org.flockdata.test.unit.batch;

import static org.junit.Assert.assertEquals;

import org.flockdata.integration.Template;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.unit.client.AbstractImport;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author mholdsworth
 * @since 6/03/2016
 */
@ActiveProfiles("dev")
public class TestFdBatcher extends AbstractImport {

    @Autowired
    private Template myBatcher;

    @Test
    public void fdBatcherAccumulation() throws Exception {
        EntityInputBean eib = new EntityInputBean(new FortressInputBean("fort"), new DocumentTypeInputBean("type"));
        eib.setCode("tt111");

        myBatcher.writeEntity(eib);
        myBatcher.writeEntity(eib);
        assertEquals(1, myBatcher.getEntities().size());

        eib = new EntityInputBean(new FortressInputBean("fort"), new DocumentTypeInputBean("Type"));
        eib.setCode("tt222");
        myBatcher.writeEntity(eib);
        myBatcher.writeEntity(eib);

        assertEquals(2, myBatcher.getEntities().size());
    }
}
