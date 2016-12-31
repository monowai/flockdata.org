/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.query.endpoint;

import org.flockdata.data.EntityLog;
import org.flockdata.data.EntityTag;
import org.flockdata.data.TxRef;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.concept.service.TxService;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityLogRlx;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.tag.EntityTagResult;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.EntityTagService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.engine.track.service.LogService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.LogDetailBean;
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

/**
 * @author mholdsworth
 * @since 4/05/2013
 * @tag Endpoint, Entity, Query
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/entity")
public class EntityEP {
    private static Logger logger = LoggerFactory.getLogger(EntityEP.class);
    private final EntityService entityService;
    private final StorageProxy storageProxy;
    private final MediationFacade mediationFacade;
    private final FortressService fortressService;
    private final EntityTagService entityTagService;
    private final TxService txService;
    private final LogService logService;

    @Autowired
    public EntityEP(LogService logService, EntityService entityService, StorageProxy storageProxy, FortressService fortressService, EntityTagService entityTagService, TxService txService, MediationFacade mediationFacade) {
        this.logService = logService;
        this.entityService = entityService;
        this.storageProxy = storageProxy;
        this.fortressService = fortressService;
        this.entityTagService = entityTagService;
        this.txService = txService;
        this.mediationFacade = mediationFacade;
    }


//    @RequestMapping(value = "/{fortress}/all/{code}", method = RequestMethod.GET)
//    public @ResponseBody Iterable<Entity> findByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("code") String code,
//                                                          HttpServletRequest request) throws FlockException {
//        Company company = CompanyResolver.resolveCompany(request);
//        return entityService.findByCallerRef(company, fortress, code);  //To change body of created methods use File | Settings | File Templates.
//    }


    @RequestMapping(value = "/{fortress}/{documentType}/{code}", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    EntityResultBean findByCode(@PathVariable("fortress") String fortressName,
                                @PathVariable("documentType") String documentType,
                                @PathVariable("code") String code,
                                HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        FortressNode fortress = fortressService.findByCode(company, fortressName);
        if ( fortress == null )
            throw new NotFoundException("Unable to locate fortress " + fortressName);
        EntityNode entity = (EntityNode)entityService.findByCode(fortress, documentType, code);
        if ( entity == null )
            throw new NotFoundException("Unable to locate entity " + code);

        return new EntityResultBean(entity);
    }

    @RequestMapping(value = "/{key}", method = RequestMethod.GET)
    public EntityResultBean getEntity(@PathVariable("key") String key,
                                      HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/entity/{key}
        EntityNode result = entityService.getEntity(company, key, true);
        if (result == null)
            throw new NotFoundException("Unable to resolve requested entity key [" + key + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return new EntityResultBean(result);
    }

    @RequestMapping(value = "/{key}/reindex", method = RequestMethod.GET)
    public String reindexEntity(@PathVariable("key") String key,
                                HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/entity/{key}
        EntityNode entity = entityService.getEntity(company, key, true);
        if (entity == null)
            throw new NotFoundException("Unable to resolve requested Entity key [" + key + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return mediationFacade.reindex(company, entity);
    }

    /**
     * locates a collection of Entity based on incoming collection of Keys
     *
     * @param toFind keys to look for
     * @return Matching entities you are authorised to receive
     * @throws FlockException duh - error
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public
    @ResponseBody
    Collection<EntityNode> getEntities(@RequestBody Collection<String> toFind,
                                       HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        return entityService.getEntities(company, toFind).values();
    }

    @RequestMapping(value = "/{key}/summary", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    EntitySummaryBean getEntitySummary(@PathVariable("key") String key,
                                       HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        return mediationFacade.getEntitySummary(company, key);

    }


    @RequestMapping(value = "/{key}/log", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Collection<EntityLogResult> getLogs(@PathVariable("key") String key,
                                        @RequestParam(value = "withData", required = false ) boolean withData,
                                        HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/entity/{key}/logs
        return entityService.getEntityLogs(company, key, withData);
    }

    @RequestMapping(value = "/{key}/log/last", produces = "application/json", method = RequestMethod.GET)
    public EntityLogResult getLastLog(@PathVariable("key") String key,
                                      HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastlog
        EntityLogRlx changed = entityService.getLastEntityLog(company, key);
        if (changed != null)
            return new EntityLogResult(changed);

        throw new NotFoundException("Unable to locate the last log for the requested key");

    }


    @RequestMapping(value = "/{key}/log/last/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTagResult> getLastLogTags(@PathVariable("key") String key,
                                               HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        return convertTags(entityService.getLastLogTags(company, key));
    }


    @RequestMapping(value = "/{key}/log/{logId}/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTagResult> getLogTags(@PathVariable("key") String key, @PathVariable("logId") long logId,
                                     HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/api/v1/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        EntityLog tl = entityService.getEntityLog(company, key, logId);
        return convertTags(entityService.getLogTags(company, tl));
    }

    private Collection<EntityTagResult> convertTags(Collection<EntityTag> logTags) {
        Collection<EntityTagResult>results = new ArrayList<>();
        for (EntityTag logTag : logTags) {
            results.add(new EntityTagResult(logTag));
        }
        return results;
    }

    @RequestMapping(value = "/{key}/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTagResult> getEntityTags(@PathVariable("key") String key,
                                        HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);

        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/{key}
        EntityNode entity = entityService.getEntity(company, key);
        return convertTags(entityTagService.findEntityTags(entity));
    }

    @RequestMapping(value = "/{key}/log/last/attachment",
            produces = "application/pdf",
            method = RequestMethod.GET)
    @ResponseBody
    public byte[] getAttachment(@PathVariable("key") String key,
                         HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        EntityNode entity = entityService.getEntity(company, key);
        if (entity != null) {
            EntityLog lastLog = logService.getLastLog(entity);
            if (lastLog == null) {
                logger.debug("Unable to find last log for {}", entity);
            } else {
                StoredContent log = storageProxy.read(entity, lastLog.getLog());
                return DatatypeConverter.parseBase64Binary(log.getAttachment());
            }
        }

        throw new NotFoundException("Unable to find the content for the requested key");

    }

//    @RequestMapping(value = "/{key}/log/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<DeltaBean> getDelta(@PathVariable("key") String key, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId,
//                                       HttpServletRequest request) throws FlockException {
//        Company company = CompanyResolver.resolveCompany(request);
//        Entity entity = entityService.getResolvedEntity(company, key);
//
//        if (entity != null) {
//            EntityLog left = entityService.getLogForEntity(entity, logId);
//            EntityLog right = entityService.getLogForEntity(entity, withId);
//            if (left != null && right != null) {
//                DeltaBean deltaBean = kvService.getDelta(entity, left.getLog(), right.getLog());
//
//                if (deltaBean != null)
//                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
//            }
//        }
//
//        throw new NotFoundException("Unable to find any content for the requested key");
//
//    }

    @RequestMapping(value = "/{key}/log/last/data", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public  Map<String, Object> getLastLogWhat(@PathVariable("key") String key,
                                       HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);

        Map<String, Object> content = entityService.getEntityDataLast(company, key);
        if (content != null) return content;

        throw new NotFoundException(String.format("Unable to locate the log for %s / lastLog", key));

    }


    @RequestMapping(value = "/{key}/log/{logId}", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    LogDetailBean getFullLog(@PathVariable("key") String key, @PathVariable("logId") Long logId,
                             HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        LogDetailBean change = entityService.getFullDetail(company, key, logId);

        if (change != null)
            return change;

        throw new NotFoundException("Unable to locate the requested log");
    }

    @RequestMapping(value = "/{key}/log/{logId}/data", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Object> getLogContent(@PathVariable("key") String key,
                                      @PathVariable("logId") Long logId,
                                      HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);

        EntityNode entity = entityService.getEntity(company, key);
        if (entity != null) {
            return mediationFacade.getLogContent(entity, logId);
        }

        throw new NotFoundException(String.format("Unable to locate the log for %s / %d", key, logId));

    }

    @RequestMapping(value = "/{key}/log/last", method = RequestMethod.DELETE)
    public ResponseEntity<String> cancelLastLog(@PathVariable("key") String key,
                                                HttpServletRequest request) throws FlockException, IOException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        EntityNode result = entityService.getEntity(company, key);
        if (result != null) {
            mediationFacade.cancelLastLog(company, result);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
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
        CompanyResolver.resolveCompany(request);
        Set<EntityNode> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = txService.findTxEntities(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map> getEntityTxLogs(@PathVariable("txRef") String txRef,
                                               HttpServletRequest request) throws FlockException {
        CompanyResolver.resolveCompany(request);
        Map<String, Object> result;
        result = txService.findByTXRef(txRef);
        if (result == null) {
            result = new HashMap<>(1);
            result.put("txRef", "Not a valid transaction identifier");
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Locate cross referenced headers by UID
     *
     * @param key  uid to start from
     * @param xRefName relationship name
     * @return all meta headers of xRefName associated with code
     * @throws FlockException  error
     */
    @RequestMapping(value = "/{key}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Collection<EntityNode>> getCrossRefence(@PathVariable("key") String key, @PathVariable("xRefName") String xRefName,
                                                        HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        return entityService.getCrossReference(company, key, xRefName);
    }

    /**
     * Locate cross referenced headers by Fortress + CallerRef
     *
     * @param fortress  name of the callers application
     * @param code unique key within the fortress
     * @param xRefName  name of the xReference to lookup
     * @return xRefName and collection of Entities
     * @throws FlockException if not exactly one CallerRef exists within the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{code}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Collection<EntityNode>> getCrossReference(@PathVariable("fortress") String fortress, @PathVariable("code") String code, @PathVariable("xRefName") String xRefName,
                                                          HttpServletRequest request) throws FlockException {
        CompanyNode company = CompanyResolver.resolveCompany(request);
        return entityService.getCrossReference(company, fortress, code, xRefName);
    }


}