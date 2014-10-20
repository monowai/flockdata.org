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

public class FlockDataClientFactoryBean extends FlockDataAbstractClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    protected FdRestWriter buildClient() throws Exception {
        Object f = properties.get("fd.fortress");
        String fortressName = null;
        if (f != null)
            fortressName = f.toString();
        Object b = properties.get("fd.batch");
        Integer batchSize = null;
        if (b != null)
            batchSize = Integer.parseInt(b.toString());
        else
            batchSize = Integer.parseInt("1");
        FdRestWriter exporter = new FdRestWriter(properties.get("server.name").toString(),
                null,
                properties.get("fd.username").toString(),
                properties.get("fd.password").toString(),
                batchSize,
                fortressName
        );
        exporter.setSimulateOnly((batchSize.intValue() <= 0));
        exporter.ensureFortress(fortressName);
        return exporter;

    }
}
