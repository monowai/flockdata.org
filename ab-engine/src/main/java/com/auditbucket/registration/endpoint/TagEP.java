package com.auditbucket.registration.endpoint;

import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@Controller
@RequestMapping("/tag")
public class TagEP {

    @Autowired
    TagService tagService;

    @Autowired
    private RegistrationService registrationService;


    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    public void createAuditTags(@RequestBody Collection<TagInputBean> input, String apiKey,
                                @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws AuditException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiKey, apiHeaderKey));
        tagService.processTags(company, input);

    }

    @ResponseBody
    @RequestMapping(value = "/{type}", produces = "application/json", consumes = "application/json", method = RequestMethod.GET)
    public Map<String, Tag> getTags(@PathVariable("type") String type) throws AuditException {
        return tagService.findTags(type);
    }
}
