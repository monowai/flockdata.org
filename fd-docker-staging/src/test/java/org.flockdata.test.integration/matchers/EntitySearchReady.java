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

package org.flockdata.test.integration.matchers;

import org.flockdata.client.commands.CommandResponse;
import org.flockdata.client.commands.EntityGet;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.transform.FdIoInterface;

/**
 * Waits for a Search count of the requested value
 *
 * @author mholdsworth
 * @since 23/04/2016
 */
public class EntitySearchReady implements ReadyMatcher {

    CommandResponse<EntityResultBean> response;
    private EntityGet entityGet;
    private EntityInputBean entityInputBean;
    private String key;
    private int waitFor;

    public EntitySearchReady(EntityGet entityGet, int searchCount, EntityInputBean entityInputBean, String key) {
        this.entityGet = entityGet;
        this.waitFor = searchCount;
        this.entityInputBean = entityInputBean;
        this.key = key;
    }

    @Override
    public boolean isReady(FdIoInterface fdIoInterface) {
        response = entityGet.exec(entityInputBean, key);
        return response.getResult() != null && response.getResult().getSearch() == waitFor;
    }

    @Override
    public CommandResponse<EntityResultBean> getResponse() {
        return response;
    }
}
