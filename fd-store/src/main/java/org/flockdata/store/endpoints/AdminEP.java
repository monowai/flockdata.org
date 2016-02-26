/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.store.endpoints;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mike on 19/02/16.
 */
@RequestMapping("${org.fd.store.system.api:api}/v1/admin")
@RestController

public class AdminEP {

    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
    String ping() throws Exception {
        // curl -X GET http://localhost:8082/api/v1/admin/ping
        return "pong";
    }
}
