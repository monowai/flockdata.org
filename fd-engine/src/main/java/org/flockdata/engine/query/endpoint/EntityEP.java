/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.engine.query.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.kv.KvContent;
import org.flockdata.kv.service.KvService;
import org.flockdata.meta.service.TxService;
import org.flockdata.model.*;
import org.flockdata.track.bean.DeltaBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.LogDetailBean;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("/entity")
public class EntityEP {
    @Autowired
    EntityService entityService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    FortressService fortressService;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    KvService kvService;

    @Autowired
    TxService txService;

    @Autowired
    LogService logService;

    private static Logger logger = LoggerFactory.getLogger(EntityEP.class);


//    @RequestMapping(value = "/{fortress}/all/{callerRef}", method = RequestMethod.GET)
//    public @ResponseBody Iterable<Entity> findByCallerRef(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef,
//                                                          HttpServletRequest request) throws FlockException {
//        Company company = CompanyResolver.resolveCompany(request);
//        return entityService.findByCallerRef(company, fortress, callerRef);  //To change body of created methods use File | Settings | File Templates.
//    }


    @RequestMapping(value = "/{fortress}/{documentType}/{callerRef}", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    EntityBean findByCallerRef(@PathVariable("fortress") String fortressName,
                               @PathVariable("documentType") String documentType,
                               @PathVariable("callerRef") String callerRef,
                               HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.findByName(company, fortressName);
        Entity entity = entityService.findByCode(fortress, documentType, callerRef);
        return new EntityBean(entity);
    }

    @RequestMapping(value = "/{metaKey}", method = RequestMethod.GET)
    public EntityBean getEntity(@PathVariable("metaKey") String metaKey,
                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/v1/track/{metaKey}
        Entity result = entityService.getEntity(company, metaKey, true);
        if (result == null)
            throw new NotFoundException("Unable to resolve requested meta key [" + metaKey + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return new EntityBean(result);
    }

    @RequestMapping(value = "/{metaKey}/reindex", method = RequestMethod.GET)
    public String reindexEntity(@PathVariable("metaKey") String metaKey,
                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/v1/track/{metaKey}
        Entity entity = entityService.getEntity(company, metaKey, true);
        if (entity == null)
            throw new NotFoundException("Unable to resolve requested meta key [" + metaKey + "]. Company is " + (company == null ? "Invalid" : "Valid"));

        return mediationFacade.reindex(company, entity);
    }

    /**
     * locates a collection of Entity based on incoming collection of MetaKeys
     *
     * @param toFind keys to look for
     * @return Matching entities you are authorised to receive
     * @throws FlockException
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public
    @ResponseBody
    Collection<Entity> getEntities(@RequestBody Collection<String> toFind,
                                   HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.getEntities(company, toFind).values();
    }

    @RequestMapping(value = "/{metaKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    EntitySummaryBean getEntitySummary(@PathVariable("metaKey") String metaKey,
                                       HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.getEntitySummary(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/log", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Set<EntityLog> getLogs(@PathVariable("metaKey") String metaKey,
                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/v1/track/{metaKey}/logs
        return entityService.getEntityLogs(company, metaKey);

    }


    @RequestMapping(value = "/{metaKey}/log/last", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<EntityLog> getLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/v1/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastlog
        EntityLog changed = entityService.getLastEntityLog(company, metaKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        throw new NotFoundException("Unable to locate the last log for the requested metaKey");

    }


    @RequestMapping(value = "/{metaKey}/log/last/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTag> getLastLogTags(@PathVariable("metaKey") String metaKey,
                                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.getLastLogTags(company, metaKey);
    }


    @RequestMapping(value = "/{metaKey}/log/{logId}/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTag> getLogTags(@PathVariable("metaKey") String metaKey, @PathVariable("logId") long logId,
                                     HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // curl -u mike:123 -X GET http://localhost:8081/v1/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        EntityLog tl = entityService.getEntityLog(company, metaKey, logId);
        return entityService.getLogTags(company, tl);
    }

    @RequestMapping(value = "/{metaKey}/tags", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<EntityTag> getEntityTags(@PathVariable("metaKey") String metaKey,
                                        HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        // curl -u mike:123 -X GET http://localhost:8081/fd-engine/track/{metaKey}
        Entity result = entityService.getEntity(company, metaKey);
        return entityTagService.getEntityTags(result);
    }

    @RequestMapping(value = "/{metaKey}/log/last/attachment",
            produces = "application/pdf",
            method = RequestMethod.GET)
    public
    @ResponseBody
    byte[] getAttachment(@PathVariable("metaKey") String metaKey,
                         HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = entityService.getEntity(company, metaKey);
        if (entity != null) {
            EntityLog lastLog = logService.getLastLog(entity);
            if (lastLog == null) {
                logger.debug("Unable to find last log for {}", entity);
            } else {
                KvContent log = kvService.getContent(entity, lastLog.getLog());
                return DatatypeConverter.parseBase64Binary(log.getAttachment());
            }
        }

        throw new NotFoundException("Unable to find the content for the requested metaKey");

    }

    @RequestMapping(value = "/{metaKey}/log/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity<DeltaBean> getDelta(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId,
                                       HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity entity = entityService.getEntity(company, metaKey);

        if (entity != null) {
            EntityLog left = entityService.getLogForEntity(entity, logId);
            EntityLog right = entityService.getLogForEntity(entity, withId);
            if (left != null && right != null) {
                DeltaBean deltaBean = kvService.getDelta(entity, left.getLog(), right.getLog());

                if (deltaBean != null)
                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
            }
        }

        throw new NotFoundException("Unable to find any content for the requested metaKey");

    }

    @RequestMapping(value = "/{metaKey}/log/{logId}", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    LogDetailBean getFullLog(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId,
                             HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        LogDetailBean change = entityService.getFullDetail(company, metaKey, logId);

        if (change != null)
            return change;

        throw new NotFoundException("Unable to locate the requested log");
    }

    @RequestMapping(value = "/{metaKey}/log/{logId}/data", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Object> getLogContent(@PathVariable("metaKey") String metaKey,
                                      @PathVariable("logId") Long logId,
                                      HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Entity entity = entityService.getEntity(company, metaKey);
        if (entity != null) {
            return mediationFacade.getLogContent(entity, logId);
        }

        throw new NotFoundException(String.format("Unable to locate the log for %s / %d", metaKey, logId));

    }

    @RequestMapping(value = "/{metaKey}/log/last", method = RequestMethod.DELETE)
    public ResponseEntity<String> cancelLastLog(@PathVariable("metaKey") String metaKey,
                                                HttpServletRequest request) throws FlockException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        Entity result = entityService.getEntity(company, metaKey);
        if (result != null) {
            mediationFacade.cancelLastLog(company, result);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/{metaKey}/log/last/data", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Object> getLastLogWhat(@PathVariable("metaKey") String metaKey,
                                       HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Entity entity = entityService.getEntity(company, metaKey);
        if (entity != null) {

            EntityLog log = entityService.getLastEntityLog(entity.getId());
            if (log != null) {
                KvContent content = kvService.getContent(entity, log.getLog());
                if (content == null)
                    throw new FlockException("Unable to locate the content for " + metaKey + ". The log was found - " + log);
                return content.getWhat();
            }
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

    /**
     * Locate cross referenced headers by UID
     *
     * @param metaKey  uid to start from
     * @param xRefName relationship name
     * @return all meta headers of xRefName associated with callerRef
     * @throws FlockException
     */
    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Collection<Entity>> getCrossRefence(@PathVariable("metaKey") String metaKey, @PathVariable("xRefName") String xRefName,
                                                    HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.getCrossReference(company, metaKey, xRefName);
    }

    /**
     * Locate cross referenced headers by Fortress + CallerRef
     *
     * @param fortress  name of the callers application
     * @param callerRef unique key within the fortress
     * @param xRefName  name of the xReference to lookup
     * @return xRefName and collection of Entities
     * @throws FlockException if not exactly one CallerRef exists within the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Collection<Entity>> getCrossReference(@PathVariable("fortress") String fortress, @PathVariable("callerRef") String callerRef, @PathVariable("xRefName") String xRefName,
                                                      HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.getCrossReference(company, fortress, callerRef, xRefName);
    }


}