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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlockDataClientFactoryBean extends FlockDataAbstractClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Value("${fd.fortress}")
    private String fortress;

    @Value("${fd.batch:1}")
    private int batch;

    @Value("${fd.server.url}")
    private String serverName;

    @Value("${fd.server.username}")
    private String userName;

    @Value("${fd.server.password}")
    private String password;

    @Override
    protected FdRestWriter buildClient() throws Exception {
        FdRestWriter exporter = new FdRestWriter(serverName,
                null,
                userName,
                password,
                batch,
                fortress
        );
        exporter.setSimulateOnly((batch <= 0));
//        exporter.ensureFortress(fortress);
        return exporter;

    }
}
