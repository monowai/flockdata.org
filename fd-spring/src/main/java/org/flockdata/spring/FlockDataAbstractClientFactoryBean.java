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

package org.flockdata.spring;

import org.flockdata.client.rest.FdRestWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


abstract class FlockDataAbstractClientFactoryBean extends FlockDataAbstractFactoryBean
        implements FactoryBean<FdRestWriter>, InitializingBean, DisposableBean {

    private final Log logger = LogFactory.getLog(getClass());

    private FdRestWriter client;

    /**
     * Implement this method to build an Flockdata client
     *
     * @return Flockdata Client
     * @throws Exception if something goes wrong
     */
    abstract protected FdRestWriter buildClient() throws Exception;


    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Starting FlockData client");
        client = buildClient();
    }

    @Override
    public void destroy() throws Exception {
        // ToDo: FixMe
//        try {
//            logger.info("Closing Flockdata client");
////            if (client != null) {
////                client.flush("");
//                //client.close();
//            }
//        } catch (final Exception e) {
//            logger.error("Error closing Flockdata client: ", e);
//        }
    }

    @Override
    public FdRestWriter getObject() throws Exception {
        return client;
    }

    @Override
    public Class<FdRestWriter> getObjectType() {
        return FdRestWriter.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Check if client is still here !
     *
     * @throws Exception
     */
    private void checkClient() throws Exception {
        if (client == null) {
            throw new Exception("Flockdata client doesn't exist. Your factory is not properly initialized.");
        }
    }

}
