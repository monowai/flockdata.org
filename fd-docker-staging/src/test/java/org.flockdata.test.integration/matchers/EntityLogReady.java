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
import org.flockdata.client.commands.EntityLogsGet;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.transform.FdIoInterface;

/**
 * @author mholdsworth
 * @since 23/04/2016
 */
public class EntityLogReady implements ReadyMatcher {

    private EntityLogsGet entityLogs;
    private int waitFor;
    private CommandResponse<EntityLogResult[]> response;
    private String key;

    public EntityLogReady(EntityLogsGet entityLogs, int waitFor, String key) {
        this.waitFor = waitFor;
        this.entityLogs = entityLogs;
        this.key = key;
    }

    @Override
    public boolean isReady(FdIoInterface fdIoInterface) {
        response = entityLogs.exec(key);

        return response.getResult() != null && response.getResult().length >= waitFor && response.getResult()[waitFor - 1].getData() != null;
    }

    @Override
    public CommandResponse<EntityLogResult[]> getResponse() {
        return response;
    }
}
