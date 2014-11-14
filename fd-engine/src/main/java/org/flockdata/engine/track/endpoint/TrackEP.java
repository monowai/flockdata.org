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

package org.flockdata.engine.track.endpoint;

import org.flockdata.engine.schema.service.TxService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.track.bean.*;
import org.flockdata.track.model.*;
import org.flockdata.track.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("/track")
public class TrackEP {
    @Autowired
    TrackService trackService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    FortressService fortressService;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    CompanyService companyService;

    @Autowired
    KvService kvService;

    @Autowired
    TxService txService;

    @Autowired
    LogService logService;

    private static Logger logger = LoggerFactory.getLogger(TrackEP.class);


    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void trackEntities(@RequestBody List<EntityInputBean> inputBeans, boolean async,
                              HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        if ( async)
            mediationFacade.trackEntitiesAsync(company, inputBeans);
        else
            mediationFacade.trackEntities(company, inputBeans);
    }

    /**
     * Tracks an entity
     *
     * @param input Entity input
     * @return TrackResultBean
     * @throws org.flockdata.helper.FlockException
     */
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public
    ResponseEntity<TrackResultBean> trackEntity(@RequestBody EntityInputBean input,
                                                HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.trackEntity(company, input);

        if ( trackResultBean.entityExists())
            if ( trackResultBean.getLogResult()!= null && trackResultBean.getLogResult().isLogIgnored())
                return new ResponseEntity<>(trackResultBean, HttpStatus.NOT_MODIFIED);

        trackResultBean.setServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.CREATED);

    }


    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<LogResultBean> trackLog(@RequestBody ContentInputBean input ,
                                                  HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);

        LogResultBean resultBean = mediationFacade.trackLog(company, input).getLogResult();
        ContentInputBean.LogStatus ls = resultBean.getStatus();
        if (ls.equals(ContentInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        else if (ls.equals(ContentInputBean.LogStatus.NOT_FOUND)) {
            throw new NotFoundException("Unable to locate the requested metaKey");
        } else if (ls.equals(ContentInputBean.LogStatus.IGNORE)) {
            input.setAbMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean, HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.PUT)
    public ResponseEntity<TrackResultBean> trackByClientRef(@RequestBody EntityInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("callerRef") String callerRef ,
                                                            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        input.setFortress(fortress);
        input.setDocumentType(recordType);
        input.setCallerRef(callerRef);
        input.setMetaKey(null);
        trackResultBean = mediationFacade.trackEntity(company, input);
        trackResultBean.setServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }


    @RequestMapping(value = "/{fortress}/all/{callerRef}", method = RequestMethod.GET)
    public @ResponseBody Iterable<Entity> findByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef,
                                                          HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.findByCallerRef(company, fortress, callerRef);  //To change body of created methods use File | Settings | File Templates.
    }


    @RequestMapping(value = "/{fortress}/{documentType}/{callerRef}", method = RequestMethod.GET)
    public @ResponseBody  Entity findByCallerRef(@PathVariable("fortress") String fortressName,
                                                 @PathVariable("documentType") String recordType,
                                                 @PathVariable("callerRef") String callerRef,
                                                 HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.findByName(company, fortressName);
        return trackService.findByCallerRef(fortress, recordType, callerRef);
    }


    @RequestMapping(value = "/{metaKey}", method = RequestMethod.GET)
    public ResponseEntity<Entity> getEntity(@PathVariable("metaKey") String metaKey ,
                                            HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/{metaKey}
        Entity result = trackService.getEntity(company, metaKey, true);
        if (result == null)
            throw new FlockException("Unable to resolve requested meta key [" + metaKey + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * locates a collection of Entity based on incoming collection of MetaKeys
     *
     * @param toFind       keys to look for
     * @return Matching entities you are authorised to receive
     * @throws org.flockdata.helper.FlockException
     */

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public @ResponseBody Collection<Entity> getEntities(@RequestBody Collection<String> toFind ,
                                                        HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getEntities(company, toFind).values();
    }

    @RequestMapping(value = "/{metaKey}/logs", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Set<EntityLog> getLogs(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/{metaKey}/logs
        return trackService.getEntityLogs(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody
    EntitySummaryBean getEntitySummary(@PathVariable("metaKey") String metaKey,
                                                            HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.getEntitySummary(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/lastlog", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<EntityLog> getLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastlog
        EntityLog changed = trackService.getLastEntityLog(company, metaKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        throw new NotFoundException("Unable to locate the last log for the requested metaKey");

    }


    @RequestMapping(value = "/{metaKey}/lastlog/tags", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Collection<EntityTag> getLastLogTags(@PathVariable("metaKey") String metaKey,
                                                             HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getLastLogTags(company, metaKey);
    }


    @RequestMapping(value = "/{metaKey}/{logId}/tags", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Collection<EntityTag> getLogTags(@PathVariable("metaKey") String metaKey, @PathVariable("logId") long logId,
                                                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        EntityLog tl = trackService.getEntityLog(company, metaKey, logId);
        return trackService.getLogTags(company, tl);
    }

    @RequestMapping(value = "/{metaKey}/tags", method = RequestMethod.GET)
    public @ResponseBody Collection<EntityTag> getEntityTags(@PathVariable("metaKey") String metaKey,
                                                            HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/{metaKey}
        Entity result = trackService.getEntity(company, metaKey);
        return entityTagService.getEntityTags(company, result);
    }

    @RequestMapping(value = "/{metaKey}/lastlog/attachment",
            produces = "application/pdf",
            method = RequestMethod.GET)
    public @ResponseBody
    byte[] getAttachment(@PathVariable("metaKey") String metaKey,
                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = trackService.getEntity(company, metaKey);
        if (entity != null) {
            EntityLog lastLog = logService.getLastLog(entity);
            if (lastLog == null) {
                logger.debug("Unable to find last log for {}", entity);
            } else {
                EntityContent log = kvService.getContent(entity, lastLog.getLog());
                return DatatypeConverter.parseBase64Binary(log.getAttachment());
            }
        }

        throw new NotFoundException("Unable to find the content for the requested metaKey") ;

    }

    @RequestMapping(value = "/{metaKey}/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<DeltaBean> getDelta(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId,
                                                            HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = trackService.getEntity(company, metaKey);

        if (entity != null) {
            EntityLog left = trackService.getLogForEntity(entity, logId);
            EntityLog right = trackService.getLogForEntity(entity, withId);
            if (left != null && right != null) {
                DeltaBean deltaBean = kvService.getDelta(entity, left.getLog(), right.getLog());

                if (deltaBean != null)
                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
            }
        }

        throw new NotFoundException("Unable to find any content for the requested metaKey") ;

    }

    @RequestMapping(value = "/{metaKey}/{logId}", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody
    LogDetailBean getFullLog(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId,
                                                  HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        LogDetailBean change = trackService.getFullDetail(company, metaKey, logId);

        if (change != null)
            return change;

        throw new NotFoundException("Unable to locate the requested log");
    }

    @RequestMapping(value = "/{metaKey}/{logId}/what", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getLogContent(@PathVariable("metaKey") String metaKey,
                                                           @PathVariable("logId") Long logId,
                                                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Entity entity = trackService.getEntity(company, metaKey);
        if (entity != null) {
            EntityLog log = trackService.getLogForEntity(entity, logId);
            if (log != null)
                return kvService.getContent(entity, log.getLog()).getWhat();
        }

        throw new NotFoundException(String.format("Unable to locate the log for %s / %d", metaKey, logId));

    }

    @RequestMapping(value = "/{metaKey}/lastlog", method = RequestMethod.DELETE)
    public ResponseEntity<String> cancelLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws FlockException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity result = trackService.getEntity(company, metaKey);
        if (result != null) {
            mediationFacade.cancelLastLog(company, result);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/{metaKey}/lastlog/what", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getLastLogWhat(@PathVariable("metaKey") String metaKey,
                                                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Entity entity = trackService.getEntity(company, metaKey);
        if (entity != null) {

            EntityLog log = trackService.getLastEntityLog(entity.getId());
            if (log != null)
                return kvService.getContent(entity, log.getLog()).getWhat();
        }

        throw new NotFoundException(String.format("Unable to locate the log for %s / lastLog", metaKey));

    }


    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef,
                                            HttpServletRequest request) throws FlockException {
        CompanyResolver.resolveCompany(request);
        TxRef result;
        result = txService.findTx(txRef);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }


    @RequestMapping(value = "/tx/{txRef}/entities", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getTransactedEntities(@PathVariable("txRef") String txRef,
                                                                     HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Set<Entity> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = txService.findTxEntities(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map> getEntityTxLogs(@PathVariable("txRef") String txRef,
                                               HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Map<String, Object> result;
        result = txService.findByTXRef(txRef);
        if (result == null) {
            result = new HashMap<>(1);
            result.put("txRef", "Not a valid transaction identifier");
            return new ResponseEntity<>((Map) result, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Map>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody Collection<String> crossReference(@PathVariable("metaKey") String metaKey, Collection<String> metaKeys, @PathVariable("xRefName") String relationshipName,
                                                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.crossReference(company, metaKey, metaKeys, relationshipName);
    }

    /**
     * Locate cross referenced headers by UID
     *
     * @param metaKey  uid to start from
     * @param xRefName relationship name
     * @return all meta headers of xRefName associated with callerRef
     * @throws org.flockdata.helper.FlockException
     */

    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody Map<String, Collection<Entity>> getCrossRefence(@PathVariable("metaKey") String metaKey, @PathVariable("xRefName") String xRefName,
                                                                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getCrossReference(company, metaKey, xRefName);
    }

    /**
     * Looks across all document types for the caller ref within the fortress. If the callerRef is not unique or does not
     * exist then an exception is thown.
     *
     * @param fortressName application
     * @param callerRef    source
     * @param entities   targets
     * @param xRefName     name of the cross reference
     * @return unresolvable caller references
     * @throws org.flockdata.helper.FlockException if not exactly one Entity for the callerRef in the fortress
     */

    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody  List<EntityKey> crossReferenceEntity(@PathVariable("fortress") String fortressName,
                                                               @PathVariable("callerRef") String callerRef,
                                                               @RequestBody Collection<EntityKey> entities,
                                                               @PathVariable("xRefName") String xRefName,
                                                               HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.crossReferenceEntities(company, new EntityKey(fortressName, "*", callerRef), entities, xRefName);
    }


    @RequestMapping(value = "/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody List<CrossReferenceInputBean> crossReferenceEntities(@RequestBody List<CrossReferenceInputBean> crossReferenceInputBeans,
                                                                              HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        return trackService.crossReferenceEntities(company, crossReferenceInputBeans);
        //return crossReferenceInputBeans;
    }


    /**
     * Locate cross referenced headers by Fortress + CallerRef
     *
     * @param fortress     name of the callers application
     * @param callerRef    unique key within the fortress
     * @param xRefName     name of the xReference to lookup
     * @return xRefName and collection of Entities
     * @throws org.flockdata.helper.FlockException if not exactly one CallerRef exists within the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public @ResponseBody  Map<String, Collection<Entity>> getCrossReference(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef, @PathVariable("xRefName") String xRefName,
                                                                            HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return trackService.getCrossReference(company, fortress, callerRef, xRefName);
    }


}