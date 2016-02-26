/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine.tag.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.track.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/tag")
public class TagEP {

    @Autowired
    TagService tagService;

    @Autowired
    MediationFacade mediationFacade;

    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Collection<TagResultBean> createTags(@RequestBody List<TagInputBean> tagInputs,
                                                HttpServletRequest request) throws FlockException, ExecutionException, InterruptedException {

        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.createTags(company, tagInputs);
    }

    @RequestMapping(value = "/{label}", produces = "application/json", method = RequestMethod.GET)
    public Collection<TagResultBean> getTags(@PathVariable("label") String label,
                                             HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Collection<Tag> tags = tagService.findTags(company, label);
        Collection<TagResultBean> results = new ArrayList<>();
        for (Tag tag : tags) {
            results.add(new TagResultBean(null, tag));
        }
        return results;
    }

    @RequestMapping(value = "/{label}/{code}", produces = "application/json", method = RequestMethod.GET)
    public TagResultBean getTag(@PathVariable("label") String label, @PathVariable("code") String code,
                                HttpServletRequest request) throws FlockException, UnsupportedEncodingException {
        Company company = CompanyResolver.resolveCompany(request);
        return new TagResultBean(tagService.findTag(company, label, null, URLDecoder.decode(code, "UTF-8"), true));
    }

    @RequestMapping(value = "/{label}/{keyPrefix}/{code}", produces = "application/json", method = RequestMethod.GET)
    public TagResultBean getTagWithPrefix(@PathVariable("label") String label,
                                          @PathVariable("keyPrefix") String keyPrefix,
                                          @PathVariable("code") String code,

                                HttpServletRequest request) throws FlockException, UnsupportedEncodingException {
        Company company = CompanyResolver.resolveCompany(request);
        return new TagResultBean(tagService.findTag(company, label,keyPrefix, URLDecoder.decode(code, "UTF-8"), true));
    }


    @RequestMapping(value = "/{label}/{sourceTag}/merge/{targetTag}", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void mergeTags(@PathVariable("sourceTag") String sourceTag, @PathVariable("targetTag") String targetTag, @PathVariable("label") String label,
                          HttpServletRequest request) throws FlockException, UnsupportedEncodingException {
        Company company = CompanyResolver.resolveCompany(request);
        Tag source = tagService.findTag(company, label, null, URLDecoder.decode(sourceTag, "UTF-8"));
        Tag target = tagService.findTag(company, label, null, URLDecoder.decode(targetTag, "UTF-8"));
        mediationFacade.mergeTags(company, source.getId(), target.getId());

    }

    @RequestMapping(value = "/{label}/{sourceTag}/alias/{akaValue}", produces = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void aliasTag(@PathVariable("sourceTag") String sourceTag, @PathVariable("akaValue") String akaValue, @PathVariable("label") String label,
                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Tag source = tagService.findTag(company, label, null , sourceTag);
        if (source == null)
            throw new NotFoundException(String.format("Unable to locate the tag {%s}/{%s}", label, sourceTag));
        mediationFacade.createAlias(company, label, source, akaValue);

    }

    @RequestMapping(value = "/{label}/{code}/alias", produces = "application/json", method = RequestMethod.GET)
    public Collection<AliasInputBean> getTagAliases(@PathVariable("label") String label, @PathVariable("code") String code,
                                                    HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return tagService.findTagAliases(company, label, null, code);
    }


    @RequestMapping(value = "/{label}/{code}/path/{relationship}/{targetLabel}", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Collection<TagResultBean>> getConnectedTags(@PathVariable("label") String label, @PathVariable("code") String code,
                                                                   HttpServletRequest request, @PathVariable("relationship") String relationship, @PathVariable("targetLabel") String targetLabel) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return tagService.findTags(company, label, code, relationship, targetLabel);
    }
}
