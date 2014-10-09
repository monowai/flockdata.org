package com.auditbucket.search.endpoint;

import com.auditbucket.search.service.SearchAdmin;
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
@RequestMapping("/admin")
@RestController
public class AdminEP {
    @Autowired
    SearchAdmin searchAdmin;

    @RequestMapping(value = "/ping", method = RequestMethod.GET)

    String ping() throws Exception {
        // curl -X GET http://localhost:8081/ab-search/v1/admin/ping
        return "Pong!";
    }

    @RequestMapping(value = "/health", produces = "application/json", method = RequestMethod.GET)

    public Map<String, Object> getHealth() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8081/ab-search/v1/admin/health
        return searchAdmin.getHealth();
    }

}
