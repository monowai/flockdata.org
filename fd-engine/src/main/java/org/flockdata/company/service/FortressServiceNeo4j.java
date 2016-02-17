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

package org.flockdata.company.service;


import org.flockdata.authentication.registration.service.SystemUserService;
import org.flockdata.company.dao.FortressDaoNeo;
import org.flockdata.configure.SecurityHelper;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.meta.dao.ConceptDaoNeo;
import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.search.IndexManager;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.FortressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Future;

@Service
@Transactional
public class FortressServiceNeo4j implements FortressService {
    private Logger logger = LoggerFactory.getLogger(FortressServiceNeo4j.class);

    @Autowired
    private FortressDaoNeo fortressDao;

    @Autowired
    private SystemUserService sysUserService;

    @Autowired
    private ConceptDaoNeo conceptDao;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private PlatformConfig engineConfig;

    @Autowired
    IndexManager indexHelper;

    @Override
    public Fortress getFortress(Long id) {
        return fortressDao.findOne(id);
    }

    @Override
    public FortressUser getUser(Long id) {
        return fortressDao.findOneUser(id);
    }

    //    @Cacheable(value = "fortressCode", unless = "#result == null")
    @Override
    public Fortress findByName(Company company, String fortressName) throws NotFoundException {
        if (fortressName == null)
            throw new NotFoundException("Unable to lookup a fortress name with a null value");
        return fortressDao.getFortressByName(company.getId(), fortressName);
    }

    @Override
    public Fortress findByName(String fortressName) throws NotFoundException {
        Company ownedBy = getCompany();
        return findByName(ownedBy, fortressName);
    }

    @Override
    public Fortress findByCode(String fortressCode) {
        Company ownedBy = getCompany();
        return findByCode(ownedBy, fortressCode);
    }

    @Override
    public Fortress findByCode(Company company, String fortressCode) {
        return fortressDao.getFortressByCode(company.getId(), fortressCode);
    }

    private Company getCompany() {
        String userName = securityHelper.getUserName(true, false);
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null) {
            throw new SecurityException("Invalid user or password");
        }
        return su.getCompany();
    }

    private Fortress save(Company company, FortressInputBean fortress) {
        return fortressDao.save(company, fortress);
    }

    /**
     * Returns an object representing the user in the supplied fortress. User is created
     * if it does not exist
     * <p/>
     * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
     * string to enforce this
     *
     * @param company      pre-authorised company
     * @param fortressUser user to locate
     * @return fortressUser identity
     */
    @Override
    public FortressUser getFortressUser(Company company, String fortressName, String fortressUser) throws NotFoundException {
        Fortress fortress = findByName(company, fortressName);
        if (fortress == null)
            return null;
        return getFortressUser(fortress, fortressUser, true);
    }

    /**
     * Returns an object representing the user in the supplied fortress. User is created
     * if it does not exist
     * <p/>
     * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
     * string to enforce this
     *
     * @param fortress     fortress to search
     * @param fortressUser user to locate
     * @return fortressUser identity
     */
    @Override
    public FortressUser getFortressUser(Fortress fortress, String fortressUser) {
        return getFortressUser(fortress, fortressUser, true);
    }

    @Override
    public FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing) {
        if (fortressUser == null || fortress == null)
            //throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUserNode]" : "FortressNode]"));
            return null;

        FortressUser result = fortressDao.getFortressUser(fortress.getId(), fortressUser.toLowerCase());
        if (createIfMissing && result == null)
            result = addFortressUser(fortress, fortressUser.toLowerCase().trim());
        return result;
    }

    private FortressUser addFortressUser(Fortress fortress, String fortressUser) {
        if (fortress == null)
            throw new IllegalArgumentException("Unable to find requested fortress");
        logger.trace("Request to add fortressUser [{}], [{}]", fortress, fortressUser);

        Company company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        return fortressDao.save(fortress, fortressUser);

    }

    @Override
    public Collection<FortressResultBean> findFortresses() throws FlockException {
        Company company = securityHelper.getCompany();
        if (company == null)
            return new ArrayList<>();
        return findFortresses(company);

    }

    @Override
    public Collection<FortressResultBean> findFortresses(Company company) throws FlockException {
        if (company == null)
            throw new FlockException("Unable to identify the requested company");
        Collection<Fortress> fortresses = fortressDao.findFortresses(company.getId());
        Collection<FortressResultBean> results = new ArrayList<>(fortresses.size());
        for (Fortress fortress : fortresses) {
            results.add(new FortressResultBean(fortress));
        }
        return results;

    }

    @Override
    public void purge(Fortress fortress) throws FlockException {
        logger.info("Purging fortress {}", fortress);
        fortressDao.delete(fortress);
        logger.info("Purged fortress {}", fortress);
    }

    /**
     * Creates a fortress if it's missing.
     *
     * @param company           who to crate for
     * @param fortressInputBean payload
     * @return existing or newly created fortress
     */
    @Override
    public Fortress registerFortress(Company company, FortressInputBean fortressInputBean) {
        return registerFortress(company, fortressInputBean, true);
    }

    @Override
    public Fortress registerFortress(Company company, String fortressName) {
        FortressInputBean fib = new FortressInputBean(fortressName,
                !engineConfig.isSearchEnabled());
        return registerFortress(company, fib, true);

    }

    @Override
    public Fortress registerFortress(Company company, FortressInputBean fib, boolean createIfMissing) {
        logger.trace("Fortress registration request {}, {}", company, fib);
        Fortress fortress = fortressDao.getFortressByCode(company.getId(), fib.getName().toLowerCase());
        boolean storeEnabled = engineConfig.storeEnabled();
        if (fortress != null) {
            if (fortress.isStoreEnabled() == null)
                // DAT-346 - data upgrade, revert to system default
                fortress.setStoreEnabled(storeEnabled);
            logger.debug("Found existing Fortress {} for Company {}", fortress, company);
            return fortress;
        }
        if (createIfMissing) {
            if (fib.getStoreActive() == null)
                fib.setStoreActive(storeEnabled);
            fortress = save(company, fib);
            logger.trace("Created fortress {}", fortress);
            fortress.setCompany(company);

            fortress.setRootIndex(indexHelper.getIndexRoot(company.getCode(), fortress.getCode()));
            logger.trace("Returning fortress {}", fortress);
            return fortress;
        }
        return null;

    }

    @Override
    public Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) throws NotFoundException {
        Fortress fortress = findByCode(company, code);
        if (fortress == null)
            fortress = findByName(company, code);
        if (fortress == null) {
            return new ArrayList<>();
        }
        Collection<DocumentResultBean> results = new ArrayList<>();
        Collection<DocumentType> rawDocs = conceptDao.getFortressDocumentsInUse(fortress);
        for (DocumentType rawDoc : rawDocs) {
            results.add(new DocumentResultBean(rawDoc));
        }
        return results;
    }

    @Override
    public Fortress getFortress(Company company, String fortressCode) throws NotFoundException {
        Fortress fortress = fortressDao.getFortressByCode(company.getId(), fortressCode);
        if (fortress == null)
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        return fortress;
    }

    @Override
    public String delete(Company company, String fortressCode) {
        Fortress fortress;
        fortress = findByCode(company, fortressCode);

        if (fortress == null)
            return "Not Found";

        return fortressDao.delete(fortress);
    }

    @Override
    @Async("fd-engine")
    public Future<Void> createFortressUsers(Fortress fortress, List<EntityInputBean> inputBeans) {
        Map<String, FortressUser> resolved = new HashMap<>();

        for (EntityInputBean inputBean : inputBeans) {
            String fu = inputBean.getFortressUser();
            if (fu != null) {
                FortressUser resolvedFu = resolved.get(fu);
                if (resolvedFu == null) {
                    resolvedFu = getFortressUser(fortress, inputBean.getFortressUser(), true);
                    resolved.put(resolvedFu.getCode(), resolvedFu);
                } else {
                    inputBean.setUser(resolvedFu);
                }
            }

        }
        return new AsyncResult<>(null);
    }

    @Override
    public FortressUser createFortressUser(Fortress fortress, ContentInputBean inputBean) {
        if (inputBean.getFortressUser() != null)
            return getFortressUser(fortress, inputBean.getFortressUser(), true);

        return null;
    }

    @Cacheable(value = "geoQuery", key = "#entity.id")
    public String getGeoQuery(Entity entity) {
        DocumentType documentType = findDocumentType(entity);
        return documentType.getGeoQuery();
    }

    public FortressSegment getDefaultSegment(Fortress fortress) {
        return fortressDao.getDefaultSegement(fortress);
    }

    @Override
    public FortressSegment addSegment(FortressSegment segment) {
        if (segment.getFortress() == null)
            throw new IllegalArgumentException("Could not associate a fortress with the segment");
//        if ( segment.getCode().equals(FortressSegment.DEFAULT))
//            throw new IllegalArgumentException("Can not use {} as the segment code", segment.getCode());
        return fortressDao.saveSegment(segment);
    }

    @Override
    public Collection<FortressSegment> getSegments(Fortress fortress) {
        return fortressDao.getSegments(fortress);
    }

    @Override
    public FortressSegment resolveSegment(Company company, EntityInputBean entityInputBean) throws NotFoundException {
        String fortressName = entityInputBean.getFortressName();
        String segmentName = entityInputBean.getSegment();

        Fortress fortress;
        FortressSegment segment;

        fortress = findByCode(company, fortressName);
        if (fortress == null)
            fortress = findByName(company, fortressName);
        if (fortress == null) {
            FortressInputBean fib = new FortressInputBean(fortressName);
            fib.setTimeZone(entityInputBean.getTimezone());
            fortress = registerFortress(company, fib, true);
            //resolvedFortresses.put(fortress.getCode(), fortress);
        }
        if (segmentName != null) {
            segment = addSegment(new FortressSegment(fortress, segmentName));
            //resolvedSegments.put(segment.getKey(), segment);
        } else {
            segment = fortress.getDefaultSegment();
        }
        return segment;

    }

    private Map<Long, EntityTagFinder> tagFinders = new HashMap<>();

    @Override
    /**
     * Returns the implementing class of an EntityTagFinder so you can provide runtime node paths
     *
     */
    public EntityService.TAG_STRUCTURE getTagStructureFinder(Entity entity) {
        EntityTagFinder tagFinder = tagFinders.get(entity.getId());
        if (tagFinder == null) {
            DocumentType documentType = findDocumentType(entity);
            if (documentType == null || documentType.getTagStructure() == null || documentType.getTagStructure() == EntityService.TAG_STRUCTURE.DEFAULT)
                return EntityService.TAG_STRUCTURE.DEFAULT; // The docType *should* exist
            if (documentType.getTagStructure() == EntityService.TAG_STRUCTURE.TAXONOMY) {
                return EntityService.TAG_STRUCTURE.TAXONOMY;
            }
        }
        return EntityService.TAG_STRUCTURE.DEFAULT;
    }

    public DocumentType findDocumentType(Entity entity) {
        Fortress f = entity.getFortress();

        if (f.getCompany() == null)
            f = getFortress(f.getId());

        String docType = entity.getType();
        return conceptDao.findDocumentType(f, docType, false);

    }
}
