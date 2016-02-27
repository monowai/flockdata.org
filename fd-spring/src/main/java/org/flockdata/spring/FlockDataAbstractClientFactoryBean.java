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

package org.flockdata.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flockdata.client.rest.FdRestWriter;
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
