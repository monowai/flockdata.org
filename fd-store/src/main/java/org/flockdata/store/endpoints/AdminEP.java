package org.flockdata.store.endpoints;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mike on 19/02/16.
 */
@RequestMapping("${fd-store.system.api:api}/v1/admin")
@RestController

public class AdminEP {

    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
    String ping() throws Exception {
        // curl -X GET http://localhost:8082/api/v1/admin/ping
        return "pong";
    }
}
