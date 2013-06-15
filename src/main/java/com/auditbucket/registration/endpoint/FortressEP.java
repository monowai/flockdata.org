package com.auditbucket.registration.endpoint;

import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * User: mike
 * Date: 1/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/fortress/")
public class FortressEP {

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SecurityHelper securityHelper;

    @RequestMapping(value = "/{fortressName}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<IFortress> getFortresses(@PathVariable("fortressName") String fortressName) throws Exception {
        // curl -u mike:123 -X GET  http://localhost:8080/ab/fortress/ABC
        IFortress fortress = fortressService.find(fortressName);
        if (fortress == null)
            return new ResponseEntity<IFortress>(fortress, HttpStatus.NOT_FOUND);
        else
            return new ResponseEntity<IFortress>(fortress, HttpStatus.OK);
    }

    @RequestMapping(value = "/new", consumes = "application/json", method = RequestMethod.PUT)
    @Transactional
    @ResponseBody
    public ResponseEntity<String> addFortresses(@RequestBody FortressInputBean fortressInputBean) throws Exception {
        IFortress fortress = fortressService.registerFortress(fortressInputBean);
        if (fortress == null)
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        else
            return new ResponseEntity<String>(fortress.getFortressKey().toString(), HttpStatus.CREATED);

    }


    @RequestMapping(value = "/{fortressName}/{user}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<IFortressUser> getFortressUsers(@PathVariable("fortressName") String fortressName, @PathVariable("userName") String userName) throws Exception {
        IFortressUser result = null;
        IFortress fortress = fortressService.find(fortressName);

        if (fortress == null) {
            return new ResponseEntity<IFortressUser>(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<IFortressUser>(fortressService.getFortressUser(fortress, userName), HttpStatus.OK);
    }

}