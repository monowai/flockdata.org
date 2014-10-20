/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/23/14
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
@Deprecated
public class DeadlockRetry {
    private static Logger logger = LoggerFactory.getLogger(DeadlockRetry.class);

    public static Command execute(Command command, String block, int maxRetry) throws FlockException, IOException, ExecutionException, InterruptedException {
        // Deadlock re-try fun
        int retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                return command.execute();
            } catch (RuntimeException re) {
                // ToDo: Exceptions getting wrapped in a JedisException. Can't directly catch the DDE hence the instanceof check
                if (re.getCause() instanceof NotFoundException || re.getCause() instanceof DeadlockDetectedException || re.getCause() instanceof InvalidDataAccessResourceUsageException || re.getCause() instanceof DataRetrievalFailureException) {
                    logger.debug("Deadlock Detected. Entering retry [{}]", block);
                    Thread.yield();
                    retryCount++;
                    if (retryCount == maxRetry) {
                        // http://www.slideshare.net/neo4j/zephyr-neo4jgraphconnect-2013short
                        //logger.error("Deadlock retries exceeded in [{}]", block);
                        throw new FlockException("Deadlock retries exceeded in "+ block, re.getCause());
                    }
                } else {
                    re.printStackTrace(); // For debugging purposes. This makes things a lot simpler to debug when an unhandled
                    // exception is thrown in a running thread.
                    logger.error("DeadlockRetry error could not be handled {}",re.getMessage());
//                    re.printStackTrace();
                    throw (re);
                }
            }
        }
        return null;
    }

}