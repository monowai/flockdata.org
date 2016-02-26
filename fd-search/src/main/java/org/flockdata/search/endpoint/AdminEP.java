/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.search.endpoint;

import org.flockdata.search.service.SearchAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * User: mike
 * Date: 5/05/14
 * Time: 9:25 AM
 */
@RequestMapping("${org.fd.search.system.api:api}/v1/admin")
@RestController
public class AdminEP {
    @Autowired
    SearchAdmin searchAdmin;

    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
    String ping() throws Exception {
        // curl -X GET http://localhost:8081/api/v1/admin/ping
        return "pong";
    }

    @RequestMapping(value = "/health", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Object> getHealth() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/admin/health
        return searchAdmin.getHealth();
    }

}
