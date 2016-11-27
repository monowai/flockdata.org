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

import org.flockdata.client.commands.EntityGet;

/**
 * Waits for a Search count of the requested value
 * @author mholdsworth
 * @since 23/04/2016
 */
public class EntitySearchReady implements ReadyMatcher {

    EntityGet entityGet;

    int waitFor;
    public EntitySearchReady(EntityGet entityGet, int searchCount) {
        this.entityGet = entityGet;
        this.waitFor = searchCount;
    }

    @Override
    public String getMessage() {
        return "EntitySearch "+waitFor;
    }

    @Override
    public boolean isReady() {
        entityGet.exec();
        return entityGet.result() != null && entityGet.result().getSearch() == waitFor;
    }
}
