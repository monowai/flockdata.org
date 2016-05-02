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

package org.flockdata.test.integration;

import org.flockdata.client.commands.EntityGet;

/**
 * Created by mike on 23/04/16.
 */
public class EntityKeyReady implements ReadyMatcher {

    EntityGet entityGet;

    public EntityKeyReady(EntityGet entityGet) {
        this.entityGet = entityGet;
    }

    @Override
    public String getMessage() {
        return "EntityKey";
    }

    @Override
    public boolean isReady() {
        entityGet.exec();
        return entityGet.result() != null && entityGet.result().getKey() != null;
    }
}
