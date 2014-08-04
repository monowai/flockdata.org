/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TagTrackService;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/track")
public class TrackEP {
    @Autowired
    TrackService trackService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    FortressService fortressService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    CompanyService companyService;

    @Autowired
    WhatService whatService;

    @Autowired
    private RegistrationService registrationService;

    private static Logger logger = LoggerFactory.getLogger(TrackEP.class);

    @ResponseBody
    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)
    public void trackHeaders(@RequestBody List<MetaInputBean> inputBeans,
                             String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, IOException {
        trackHeaders(inputBeans, false, ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
    }


    public int trackHeaders(List<MetaInputBean> inputBeans, boolean async, String apiKey) throws DatagioException, IOException {
        Company company = registrationService.resolveCompany(apiKey);
        Fortress fortress = fortressService.registerFortress(company, new FortressInputBean(inputBeans.iterator().next().getFortress()), true);
        if (async) {
            Future<Integer> batch = mediationFacade.createHeadersAsync(company, fortress, inputBeans);
            Thread.yield();
            try {
                return batch.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Unexpected", e);
            }
            return 0;

        } else {
            return mediationFacade.createHeaders(company, fortress, inputBeans, inputBeans.size());
        }

    }

    /**
     * Creates a header
     *
     * @param input meta header input
     * @return TrackResultBean
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public ResponseEntity<TrackResultBean> trackHeader(@RequestBody MetaInputBean input,
                                                       String apiKey,
                                                       @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, IOException {
        // curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8081/ab-engine/track/track/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "documentType":"CompanyNode","when":"2012-11-10"}'

        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.createHeader(input, ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        trackResultBean.setServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<LogResultBean> trackLog(@RequestBody LogInputBean input, String apiKey,
                                                  @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, IOException {

        // If we have a valid company we are good to go.
        Company company = getCompany(apiHeaderKey, apiKey);

        LogResultBean resultBean = mediationFacade.processLog(company, input).getLogResult();
        LogInputBean.LogStatus ls = resultBean.getStatus();
        if (ls.equals(LogInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        else if (ls.equals(LogInputBean.LogStatus.NOT_FOUND)) {
            input.setAbMessage("Illegal meta key");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_FOUND);
        } else if (ls.equals(LogInputBean.LogStatus.IGNORE)) {
            input.setAbMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(LogInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.PUT)
    public ResponseEntity<TrackResultBean> trackByClientRef(@RequestBody MetaInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("callerRef") String callerRef,
                                                            String apiKey,
                                                            @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, IOException {

        TrackResultBean trackResultBean;
        input.setFortress(fortress);
        input.setDocumentType(recordType);
        input.setCallerRef(callerRef);
        input.setMetaKey(null);
        trackResultBean = mediationFacade.createHeader(input, ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        trackResultBean.setServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/all/{callerRef}", method = RequestMethod.GET)
    public Iterable<MetaHeader> getByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef, String apiKey,
                                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.findByCallerRef(company, fortress, callerRef);  //To change body of created methods use File | Settings | File Templates.
    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{documentType}/{callerRef}", method = RequestMethod.GET)
    public MetaHeader getByCallerRef(@PathVariable("fortress") String fortressName,
                                     @PathVariable("documentType") String recordType,
                                     @PathVariable("callerRef") String callerRef, String apiKey,
                                     @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        Fortress fortress = fortressService.findByName(company, fortressName);
        MetaHeader result = trackService.findByCallerRef(fortress, recordType, callerRef);
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}", method = RequestMethod.GET)
    public ResponseEntity<MetaHeader> getMetaHeader(@PathVariable("metaKey") String metaKey,
                                                    String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        MetaHeader result = trackService.getHeader(company, metaKey, true);
        if (result == null)
            throw new DatagioException("Unable to resolve requested meta key [" + metaKey + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * locates a collection of MetaHeaders based on incoming collection of MetaKeys
     *
     * @param toFind       keys to look for
     * @param apiKey       who you purport to be
     * @param apiHeaderKey as per apiKey but in the header
     * @return Matching metaHeaders you are authorised to receive
     * @throws DatagioException
     */
    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Collection<MetaHeader> getMetaHeaders(@RequestBody Collection<String> toFind,
                                                 String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.getHeaders(company, toFind);
    }


    private Company getCompany(String apiHeaderKey, String apiRequestKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiRequestKey));
        if (company == null)
            throw new DatagioException("Unable to resolve supplied API key to a valid company");
        return company;
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/logs", produces = "application/json", method = RequestMethod.GET)
    public Set<TrackLog> getLogs(@PathVariable("metaKey") String metaKey,
                                 String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}/logs
        return trackService.getLogs(company, metaKey);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TrackedSummaryBean> getAuditSummary(@PathVariable("metaKey") String metaKey,
                                                              String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return new ResponseEntity<>(mediationFacade.getTrackedSummary(company, metaKey), HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TrackLog> getLastChange(@PathVariable("metaKey") String metaKey, String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        TrackLog changed = trackService.getLastLog(company, metaKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        return new ResponseEntity<>((TrackLog) null, HttpStatus.NOT_FOUND);

    }


    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog/tags", produces = "application/json", method = RequestMethod.GET)
    public Set<Tag> getLastChangeTags(@PathVariable("metaKey") String metaKey, String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
//        TrackLog changed = trackService.getLastLog(company, metaKey);
        return trackService.getLastLogTags(company, metaKey);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}/tags", produces = "application/json", method = RequestMethod.GET)
    public Set<Tag> getChangeTags(@PathVariable("metaKey") String metaKey, @PathVariable("logId")long logId,
                                  String apiKey, @RequestHeader(value = "Api-Key", required = false)
    String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
//        TrackLog changed = trackService.getLastLog(company, metaKey);
        TrackLog tl = trackService.getLog(company, metaKey, logId);

        return trackService.getLogTags(company, tl);

    }


    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog/what", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<LogWhat> getLastChangeWhat(@PathVariable("metaKey") String metaKey, String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        MetaHeader header = trackService.getHeader(company, metaKey);
        if (header != null) {
            TrackLog lastLog = trackService.getLastLog(header);
            if (lastLog == null) {
                logger.debug("Unable to find last log for {}", header);
            } else {
                LogWhat what = whatService.getWhat(header, lastLog.getLog());
                return new ResponseEntity<>(what, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>((LogWhat) null, HttpStatus.NOT_FOUND);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_AB_ADMIN"})
    public ResponseEntity<AuditDeltaBean> getDelta(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        MetaHeader header = trackService.getHeader(company, metaKey);

        if (header != null) {
            TrackLog left = trackService.getLogForHeader(header, logId);
            TrackLog right = trackService.getLogForHeader(header, withId);
            if (left != null && right != null) {
                AuditDeltaBean deltaBean = whatService.getDelta(header, left.getLog(), right.getLog());

                if (deltaBean != null)
                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>((AuditDeltaBean) null, HttpStatus.NOT_FOUND);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<LogDetailBean> getFullLog(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        LogDetailBean change = trackService.getFullDetail(company, metaKey, logId);

        if (change != null)
            return new ResponseEntity<>(change, HttpStatus.OK);

        return new ResponseEntity<>((LogDetailBean) null, HttpStatus.NOT_FOUND);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}/what", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<LogWhat> getLogWhat(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);

        MetaHeader header = trackService.getHeader(company, metaKey);
        if (header != null) {
            TrackLog log = trackService.getLogForHeader(header, logId);
            if (log != null)
                return new ResponseEntity<>(whatService.getWhat(header, log.getLog()), HttpStatus.OK);
        }

        return new ResponseEntity<>((LogWhat) null, HttpStatus.NOT_FOUND);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/tags", method = RequestMethod.GET)
    public Set<TrackTag> getAuditTags(@PathVariable("metaKey") String metaKey
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);

        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        MetaHeader result = trackService.getHeader(company, metaKey);
        return tagTrackService.findTrackTags(company, result);
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog", method = RequestMethod.DELETE)
    public ResponseEntity<String> cancelLastLog(@PathVariable("metaKey") String metaKey,
                                                String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, IOException {

        Company company = getCompany(apiHeaderKey, apiKey);
        MetaHeader result = trackService.getHeader(company, metaKey);
        if (result != null) {
            mediationFacade.cancelLastLogSync(result.getMetaKey());
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);

    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_AB_ADMIN"})
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        TxRef result;
        result = trackService.findTx(txRef);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_AB_ADMIN"})
    public ResponseEntity<Map<String, Object>> getAuditTxHeaders(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        Set<MetaHeader> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = trackService.findTxHeaders(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_AB_ADMIN"})
    public ResponseEntity<Map> getAuditTxLogs(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/tx/{txRef}/logs
        Map<String, Object> result;
        result = trackService.findByTXRef(txRef);
        if (result == null) {
            result = new HashMap<>(1);
            result.put("txRef", "Not a valid transaction identifier");
            return new ResponseEntity<>((Map) result, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Map>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public Collection<String> putCrossReference(@PathVariable("metaKey") String metaKey, Collection<String> metaKeys, @PathVariable("xRefName") String relationshipName,
                                                String apiKey,
                                                @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.crossReference(company, metaKey, metaKeys, relationshipName);
    }

    /**
     * Locate cross referenced headers by UID
     *
     * @param metaKey      uid to start from
     * @param xRefName     relationship name
     * @param apiKey       apiKey
     * @param apiHeaderKey headerKey
     * @return all meta headers of xRefName associated with callerRef
     * @throws DatagioException
     */
    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Collection<MetaHeader>> getCrossRefenceByMetaKey(@PathVariable("metaKey") String metaKey, @PathVariable("xRefName") String xRefName, String apiKey,
                                                                        @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.getCrossReference(company, metaKey, xRefName);
    }

    /**
     * Looks across all document types for the caller ref within the fortress. If the callerRef is not unique or does not
     * exist then an exception is thown.
     *
     * @param fortressName application
     * @param callerRef    source
     * @param callerRefs   targets
     * @param xRefName     name of the cross reference
     * @param apiKey       {optional} authorised key
     * @param apiHeaderKey {optional} authorised key
     * @return unresolvable caller references
     * @throws DatagioException if not exactly one MetaHeader for the callerRef in the fortress
     */
    @ResponseBody
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public List<MetaKey> postCrossReferenceByCallerRef(@PathVariable("fortress") String fortressName, @PathVariable("callerRef") String callerRef,
                                                       @RequestBody Collection<MetaKey> callerRefs,
                                                       @PathVariable("xRefName") String xRefName,
                                                       String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.crossReferenceByCallerRef(company, new MetaKey(fortressName, "*", callerRef), callerRefs, xRefName);
    }

    @ResponseBody
    @RequestMapping(value = "/xref", produces = "application/json", method = RequestMethod.POST)
    public List<CrossReferenceInputBean> postCrossReferenceByCallerRef(@RequestBody List<CrossReferenceInputBean> crossReferenceInputBeans,
                                                                       String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey)
            throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);

        for (CrossReferenceInputBean crossReferenceInputBean : crossReferenceInputBeans) {
            Map<String, List<MetaKey>> references = crossReferenceInputBean.getReferences();
            for (String xRefName : references.keySet()) {
                try {
                    List<MetaKey> notFound = trackService.crossReferenceByCallerRef(company,
                            new MetaKey(crossReferenceInputBean.getFortress(),
                                    crossReferenceInputBean.getDocumentType(),
                                    crossReferenceInputBean.getCallerRef()),
                            references.get(xRefName), xRefName);
                    crossReferenceInputBean.setIgnored(xRefName, notFound);
//                    references.put(xRefName, notFound);
                } catch (DatagioException de) {
                    logger.error("Exception while cross-referencing MetaHeaders. This message is being returned to the caller - [{}]", de.getMessage());
                    crossReferenceInputBean.setServiceMessage(de.getMessage());
                }
            }
        }
        return crossReferenceInputBeans;
    }


    /**
     * Locate cross referenced headers by Fortress + CallerRef
     *
     * @param fortress     name of the callers application
     * @param callerRef    unique key within the fortress
     * @param xRefName     name of the xReference to lookup
     * @param apiKey       apikey
     * @param apiHeaderKey apiKey by header
     * @return xRefName and collection of MetaHeaders
     * @throws DatagioException if not exactly one CallerRef exists within the fortress
     */
    @ResponseBody
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public Map<String, Collection<MetaHeader>> getCrossReferenceByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef, @PathVariable("xRefName") String xRefName
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        return trackService.getCrossReference(company, fortress, callerRef, xRefName);
    }


}