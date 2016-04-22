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

import me.tongfei.progressbar.ProgressBar;
import org.flockdata.client.commands.Command;
import org.flockdata.client.commands.EntityGet;
import org.flockdata.client.commands.EntityLogsGet;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Integration utils. Keeps generic functionality out of the IT class
 *
 * Created by mike on 20/04/16.
 */
@Service
@Configuration
class IntegrationHelper {

    private static Logger logger = LoggerFactory.getLogger(IntegrationHelper.class);

    @Value("${org.fd.test.sleep:1000}")
    private int sleep;

    @Value("${org.fd.test.attempts:100}")
    private int attempts;

    Collection<EntityInputBean> toCollection(EntityInputBean entityInputBean) throws IOException {
        Collection<EntityInputBean> entities = new ArrayList<>();
        entities.add(entityInputBean);
        return entities;
    }

    void waitUntil(ReadyMatcher readyMatcher) {
        ProgressBar pb = null;
        StopWatch watch = new StopWatch();
        watch.start(readyMatcher.getMessage());
        int count = 0;
        boolean ready = false;

        do {
            try {
                ready = readyMatcher.isReady();
                if ( !ready ) {
                    if ( pb == null && count > 5 ) {
                        pb = new ProgressBar(readyMatcher.getMessage(), attempts -5);
                        pb.start();
                    }

                    Thread.sleep(sleep);
                    if ( pb!=null )
                        pb.stepBy(1);
                    count ++;

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while ( count < attempts && !ready);
        if ( count == attempts ) {
            logger.error("Timeout of {} was hit before we got a result for {}", attempts, readyMatcher.getMessage());
        }
        watch.stop();
        logger.info(watch.prettyPrint());

    }

    void waitForEntityLog(EntityLogsGet entityLogs, int waitFor) {
        EntityLogReady waiter = new EntityLogReady(entityLogs, waitFor);
        waitUntil(waiter);
    }

    // Executes a GetEntity command and waits for a result. Can take some time depending on the environment that this
    // is working on.
    void waitForEntityKey(EntityGet entityGet) {
        EntityKeyReady waiter = new EntityKeyReady(entityGet);
        waitUntil(waiter);
    }

    void waitForSearch(EntityGet entityGet, int searchCount) {
        EntitySearchReady waiter = new EntitySearchReady(entityGet, searchCount);
        waitUntil(waiter);
    }

    void pauseUntil(Command optionalCommand, String commandResult, int waitCount) throws InterruptedException {
        // A nice little status bar to show how long we've been hanging around
        ProgressBar pb = new ProgressBar("Looking for services.... ", waitCount);
        pb.start();
        int run = 0;
        do {
            run++;
            pb.step();
            // After waiting for 40% of the waitCount will try running the command if it exists
            if (optionalCommand != null && run % 10 == 0 && (((double) run) / waitCount) > .3) {
                // After 1 minute we will ping to see if we can finish this early
                String result = optionalCommand.exec();
                if (result.equals(commandResult)) {
                    // We can finish early
                    pb.stepBy((waitCount - run));
                    return;

                }
            } else {
                Thread.sleep(1000);
            }

        } while (run != waitCount);
    }


}
