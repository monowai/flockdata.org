package com.auditbucket.helper;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/23/14
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeadlockRetry {
    private static Logger logger = LoggerFactory.getLogger(DeadlockRetry.class);

    public static Command execute(Command command, int maxRetry) {
        // Deadlock re-try fun
        int retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                return command.execute();
            } catch (RuntimeException re) {
                // ToDo: Exceptions getting wrapped in a JedisException. Can't directly catch the DDE hence the instanceof check
                if (re.getCause() instanceof NotFoundException || re.getCause() instanceof DeadlockDetectedException || re.getCause() instanceof InvalidDataAccessResourceUsageException || re.getCause() instanceof DataRetrievalFailureException) {
                    logger.debug("Deadlock Detected. Entering retry");
                    Thread.yield();
                    retryCount++;
                    if (retryCount == maxRetry) {
                        // http://www.slideshare.net/neo4j/zephyr-neo4jgraphconnect-2013short
                        logger.error("Deadlock retries exceeded");
                        throw (re);
                    }
                } else {
                    throw (re);
                }
            }
        }
        return null;
    }

}
