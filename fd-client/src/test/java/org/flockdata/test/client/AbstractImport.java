/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import org.flockdata.transform.ClientConfiguration;

/**
 * Simple ancestor for encapsulating profile and writer functionality
 *
 * Created by mike on 12/02/15.
 */

public class AbstractImport {
    // Re-implement an FdWriter class if you want to validate data in the flush routines

    private static MockFdWriter fdWriter = new MockFdWriter();

    protected ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setBatchSize(100);
        return clientConfiguration;
    }

    public static MockFdWriter getFdWriter() {
        return fdWriter;
    }

}
