/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flockdata.client.rest.FdRestWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlockDataClientFactoryBean extends FlockDataAbstractClientFactoryBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Value("${org.fd.fortress}")
    private String fortress;

    @Value("${org.fd.batch:1}")
    private int batch;

    @Value("${org.fd.engine.api}")
    private String serverName;

    @Value("${org.fd.server.username}")
    private String userName;

    @Value("${org.fd.server.password}")
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
