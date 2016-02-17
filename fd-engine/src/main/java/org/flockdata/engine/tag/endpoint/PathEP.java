package org.flockdata.engine.tag.endpoint;

import org.flockdata.engine.tag.service.TagPath;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Map;

/**
 * Created by mike on 28/12/15.
 */

@RestController
@RequestMapping("/v1/path")
public class PathEP {

    @Autowired
    TagPath tagPath;

    @RequestMapping(value = "/{label}/{code}/{length}/{targetLabel}", produces = "application/json", method = RequestMethod.GET)
    public Collection<Map<String, Object>> getConnectedTags(@PathVariable("label") String label, @PathVariable("code") String code,
                                                            HttpServletRequest request, @PathVariable("targetLabel") String targetLabel, @PathVariable("length") Integer length) throws FlockException, UnsupportedEncodingException {
        Company company = CompanyResolver.resolveCompany(request);
        return tagPath.getPaths(company, URLDecoder.decode(label,"UTF-8"), URLDecoder.decode(code,"UTF-8"), length, URLDecoder.decode(targetLabel,"UTF-8"));
//        return tagService.findTags(company, label, code, relationship, targetLabel);
    }


}
