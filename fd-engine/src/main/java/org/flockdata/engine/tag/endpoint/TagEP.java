/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.track.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@RestController
@RequestMapping("/tag")
public class TagEP {

    @Autowired
    TagService tagService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    MediationFacade mediationFacade;

    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Collection<TagResultBean> createTags(@RequestBody List<TagInputBean> tagInputs,
                                                HttpServletRequest request) throws FlockException, ExecutionException, InterruptedException {
        Company company = CompanyResolver.resolveCompany(request);

        return mediationFacade.createTags(company, tagInputs);

    }

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.DELETE)
    public ResponseEntity<String> purgeUnusedConcepts(HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        tagService.purgeUnusedConcepts(company);
        return new ResponseEntity<>("Purged unused concepts", HttpStatus.ACCEPTED);

    }

    @RequestMapping(value = "/{label}", produces = "application/json", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteConcepts( @PathVariable("label") String label,
                                                  HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        tagService.purgeLabel(company, label);
        return new ResponseEntity<>("Purged unused concepts", HttpStatus.ACCEPTED);

    }

    @RequestMapping(value = "/{type}", produces = "application/json", method = RequestMethod.GET)
    public Collection<TagResultBean> getTags(@PathVariable("type") String index,
                                             HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Collection<Tag> tags = tagService.findTags(company, index);
        Collection<TagResultBean> results = new ArrayList<>();
        for (Tag tag : tags) {
            results.add(new TagResultBean(null, tag));
        }
        return results;
    }

    @RequestMapping(value = "/{label}/{code}", produces = "application/json",  method = RequestMethod.GET)
    public TagResultBean getTag(@PathVariable("label") String label,  @PathVariable("code") String code,
                                HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return new TagResultBean(tagService.findTag(company, label, code));
    }

    @RequestMapping(value = "/{label}/{sourceTag}/merge/{targetTag}", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void mergeTags(@PathVariable("sourceTag") String sourceTag, @PathVariable("targetTag") String targetTag, @PathVariable("label") String label,
                          HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Tag source = tagService.findTag(company, label, sourceTag);
        Tag target = tagService.findTag(company, label, targetTag);
        mediationFacade.mergeTags(company, source, target);

    }

    @RequestMapping(value = "/{label}/{sourceTag}/alias/{akaValue}", produces = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void aliasTag( @PathVariable("sourceTag") String sourceTag, @PathVariable("akaValue") String akaValue, @PathVariable("label") String label,                                           HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Tag source = tagService.findTag(company, label, sourceTag);
        if ( source == null )
            throw new NotFoundException(String.format("Unable to locate the tag {%s}/{%s}", label, sourceTag));
        mediationFacade.createAlias(company, label, source, akaValue);

    }

    @RequestMapping(value = "/{label}/{code}/alias", produces = "application/json", method = RequestMethod.GET)
    public Collection<AliasInputBean> getTagAliases(@PathVariable("label") String label, @PathVariable("code") String code,
                                                    HttpServletRequest request)  throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return tagService.findTagAliases(company, label, code);
    }

}
