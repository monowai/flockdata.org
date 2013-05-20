package com.auditbucket.registration.endpoint;

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: mike
 * Date: 1/05/13
 * Time: 8:23 PM
 */
@Controller
// Customise a dispatcher in web.xml
@RequestMapping("/")
public class RegistrationEP {

    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/register", consumes = "application/json", method = RequestMethod.PUT)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<ISystemUser> register(@RequestBody RegistrationBean regBean) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/profiles/register -d '{"name":"mikey", "companyName":"Monowai Dev","password":"whocares"}'
        ISystemUser su = regService.registerSystemUser(regBean);
        if (su == null)
            return new ResponseEntity<ISystemUser>(su, HttpStatus.INTERNAL_SERVER_ERROR);

        return new ResponseEntity<ISystemUser>(su, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResponseBody
    public ISystemUser get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/profiles/me
        ISystemUser result = regService.getSystemUser();

        return result;
    }


}