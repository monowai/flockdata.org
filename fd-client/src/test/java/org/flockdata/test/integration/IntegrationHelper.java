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

    EntityLogsGet waitForEntityLog(EntityLogsGet entityLogs, int waitFor) {
        ProgressBar pb = null;
        StopWatch watch = new StopWatch("waitForEntityLog");
        watch.start("waitForEntityLog");
        int count = 0;
        boolean found = false;
        entityLogs.exec();
        do {
            try {
                found = entityLogs.getResult() != null && entityLogs.getResult().length == waitFor && entityLogs.getResult()[waitFor-1].getData()!=null;
                if ( !found ) {
                    if ( pb == null && count > 5 ) {
                        pb = new ProgressBar("Waiting for entity Log.... ", attempts -5);
                        pb.start();
                    }

                    Thread.sleep(sleep);
                    if ( pb!=null )
                        pb.stepBy(1);
                    count ++;
                    entityLogs.exec();

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while ( count < attempts && !found);
        if ( count == attempts ) {
            logger.error("Timeout of {} was hit before we got a result", attempts);
        }
        watch.stop();
        logger.info(watch.prettyPrint());

        return entityLogs;
    }

    // Executes a GetEntity command and waits for a result. Can take some time depending on the environment that this
    // is working on.
    EntityGet waitForEntityKey(EntityGet entityGet) {
        int count = 0;
        boolean found = false;
        ProgressBar pb = null;
        StopWatch watch = new StopWatch("waitForEntityKey");
        watch.start("waitForEntityLog");

        do {
            try {
                found = entityGet.getResult() != null && entityGet.getResult().getKey() != null;
                if ( !found ) {
                    if ( pb == null && count > 5 ) {
                        pb = new ProgressBar("Waiting for entity Key.... ", attempts-5);
                        pb.start();
                    }
                    Thread.sleep(sleep);
                    if ( pb !=null)
                        pb.stepBy(1);
                    count++;
                    entityGet.exec();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while ( count < attempts && !found);

        if ( count == attempts && !found)
            logger.error ( "Timeout of {} was hit before we got a result" ,attempts);

        watch.stop();
        logger.info(watch.prettyPrint());

        return entityGet;
    }

    EntityGet waitForSearch(EntityGet entityGet, int searchCount) {
        int count = 0;
        boolean found = false;
        ProgressBar pb = null;
        StopWatch watch = new StopWatch("waitForSearch");
        watch.start("waitForSearch");

        do {
            try {
                found = entityGet.getResult() != null && entityGet.getResult().getSearch() == searchCount;
                if ( !found ) {
                    if ( pb == null && count > 5 ) {
                        pb = new ProgressBar("Waiting for search change.... ", attempts-5);
                        pb.start();
                    }
                    Thread.sleep(sleep);
                    if ( pb !=null)
                        pb.stepBy(1);
                    count++;
                    entityGet.exec();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while ( count < attempts && !found);

        if ( count == attempts && !found)
            logger.error ( "Timeout of {} was hit before we got a result" ,attempts);

        watch.stop();
        logger.info(watch.prettyPrint());

        return entityGet;
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
