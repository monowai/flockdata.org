package com.auditbucket.registration.endpoint;

import com.auditbucket.bean.TagInputBean;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@Controller
@RequestMapping("/tag")
public class TagEP {

    @Autowired
    TagService tagService;

    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    public void createAuditTags(@RequestBody TagInputBean[] input) throws AuditException {
        for (TagInputBean inputBean : input) {
            tagService.processTag(inputBean);
        }
    }
}
