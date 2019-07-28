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

package org.flockdata.company.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.authentication.SystemUserService;
import org.flockdata.company.dao.FortressDaoNeo;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.data.dao.ConceptDaoNeo;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressSegmentNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FortressServiceNeo4j implements FortressService {
  private final FortressDaoNeo fortressDao;
  private final SystemUserService sysUserService;
  private final ConceptDaoNeo conceptDao;
  private final SecurityHelper securityHelper;
  private final PlatformConfig engineConfig;
  private final IndexManager indexManager;
  private Logger logger = LoggerFactory.getLogger(FortressServiceNeo4j.class);
  private Map<Long, EntityTagFinder> tagFinders = new HashMap<>();

  @Autowired
  public FortressServiceNeo4j(FortressDaoNeo fortressDao, ConceptDaoNeo conceptDao, IndexManager indexManager, SystemUserService sysUserService, PlatformConfig engineConfig, SecurityHelper securityHelper) {
    this.fortressDao = fortressDao;
    this.conceptDao = conceptDao;
    this.indexManager = indexManager;
    this.sysUserService = sysUserService;
    this.engineConfig = engineConfig;
    this.securityHelper = securityHelper;
  }

  @Override
  public FortressNode getFortress(Long id) {
    return fortressDao.findOne(id);
  }

  @Override
  public FortressUserNode getUser(Long id) {
    return fortressDao.findOneUser(id);
  }

  //    @Cacheable(value = "fortressCode", unless = "#result == null")
  @Override
  public FortressNode findByName(Company company, String fortressName) throws NotFoundException {
    if (fortressName == null) {
      throw new NotFoundException("Unable to lookup a fortress name with a null value");
    }
    return fortressDao.getFortressByName(company.getId(), fortressName);
  }

  @Override
  public FortressNode findByName(String fortressName) throws NotFoundException {
    return findByName(getCompany(), fortressName);
  }

  public FortressNode findByCode(String fortressCode) {
    return findByCode(getCompany(), fortressCode);
  }

  @Override
  public FortressNode findByCode(Company company, String fortressCode) {
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

  private FortressNode save(Company company, FortressInputBean fortress) {
    return fortressDao.save(company, fortress);
  }

  /**
   * Returns an object representing the user in the supplied fortress. User is created
   * if it does not exist
   * <p>
   * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
   * string to enforce this
   *
   * @param company      pre-authorised company
   * @param fortressUser user to locate
   * @return fortressUser identity
   */
  @Override
  public FortressUserNode getFortressUser(Company company, String fortressName, String fortressUser) throws NotFoundException {
    FortressNode fortress = findByName(company, fortressName);
    if (fortress == null) {
      return null;
    }
    return getFortressUser(fortress, fortressUser, true);
  }

  /**
   * Returns an object representing the user in the supplied fortress. User is created
   * if it does not exist
   * <p>
   * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
   * string to enforce this
   *
   * @param fortress     fortress to search
   * @param fortressUser user to locate
   * @return fortressUser identity
   */
  @Override
  public FortressUserNode getFortressUser(Fortress fortress, String fortressUser) {
    return getFortressUser(fortress, fortressUser, true);
  }

  @Override
  public FortressUserNode getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing) {
    if (fortressUser == null || fortress == null)
    //throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUserNode]" : "FortressNode]"));
    {
      return null;
    }

    FortressUserNode result = fortressDao.getFortressUser(fortress.getId(), fortressUser.toLowerCase());
    if (createIfMissing && result == null) {
      result = addFortressUser(fortress, fortressUser.toLowerCase().trim());
    }
    return result;
  }

  private FortressUserNode addFortressUser(Fortress fortress, String fortressUser) {
    if (fortress == null) {
      throw new IllegalArgumentException("Unable to find requested fortress");
    }
    logger.trace("Request to add fortressUser [{}], [{}]", fortress, fortressUser);

    Company company = fortress.getCompany();
    // this should never happen
    if (company == null) {
      throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");
    }

    return fortressDao.save((FortressNode) fortress, fortressUser);

  }

  @Override
  public Collection<FortressResultBean> findFortresses() throws FlockException {
    Company company = securityHelper.getCompany();
    if (company == null) {
      return new ArrayList<>();
    }
    return findFortresses(company);

  }

  @Override
  public Collection<FortressResultBean> findFortresses(Company company) throws FlockException {
    if (company == null) {
      throw new FlockException("Unable to identify the requested company");
    }
    Collection<FortressNode> fortresses = fortressDao.findFortresses(company.getId());
    Collection<FortressResultBean> results = new ArrayList<>(fortresses.size());
    for (FortressNode fortress : fortresses) {
      if (!fortress.isSystem()) {
        results.add(new FortressResultBean(fortress));
      }
    }
    return results;

  }

  @Override
  public void purge(Fortress fortress) throws FlockException {
    logger.info("Purging fortress {}", fortress);
    fortressDao.purgeFortress(fortress.getId());
    Collection<DocumentNode> docTypes = conceptDao.getFortressDocumentsInUse(fortress);
    for (DocumentNode docType : docTypes) {
      logger.debug("Deleting DocType {}", docType);
      conceptDao.delete(docType.getId());
    }

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
  public FortressNode registerFortress(Company company, FortressInputBean fortressInputBean) {
    return registerFortress(company, fortressInputBean, true);
  }

  @Override
  public FortressNode registerFortress(Company company, String fortressName) {
    FortressInputBean fib = new FortressInputBean(fortressName,
        !engineConfig.isSearchEnabled());
    return registerFortress(company, fib, true);

  }

  @Override
  public FortressNode registerFortress(Company company, FortressInputBean fib, boolean createIfMissing) {
    logger.debug("Fortress registration request {}, {}", company, fib);
    FortressNode fortress = fortressDao.getFortressByCode(company.getId(), fib.getCode());
    boolean storeEnabled = engineConfig.storeEnabled();
    if (fortress != null) {
      if (fib.isStoreEnabled() != null) {
        fortress.setStoreEnabled(fib.isStoreEnabled());
      } else {
        fortress.setStoreEnabled(storeEnabled);
      }

      if (fib.isSearchEnabled() != null) {
        fortress.setSearchEnabled(fib.isSearchEnabled());
      }

      logger.debug("Found existing Fortress {} for Company {}", fortress, company);
      fortressDao.save(fortress);
      return fortress;
    }
    if (createIfMissing) {
      if (fib.isStoreEnabled() == null) {
        fib.setStoreEnabled(storeEnabled);
      }
      fortress = save(company, fib);
      logger.debug("Created fortress {}", fortress);
      fortress.setCompany(company);

      fortress.setRootIndex(indexManager.getIndexRoot(fortress));
      logger.trace("Returning fortress {}", fortress);
      return fortress;
    }
    return null;

  }

  @Override
  public Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) throws NotFoundException {
    FortressNode fortress = findByCode(company, code);
    if (fortress == null) {
      fortress = findByName(company, code);
    }
    if (fortress == null) {
      return new ArrayList<>();
    }
    Collection<DocumentResultBean> results = new ArrayList<>();
    Collection<DocumentNode> rawDocs = conceptDao.getFortressDocumentsInUse(fortress);
    for (DocumentNode rawDoc : rawDocs) {
      rawDoc = conceptDao.findDocumentTypeWithSegments(rawDoc);
      results.add(new DocumentResultBean(rawDoc, fortress));
    }
    return results;
  }

  @Override
  public Fortress getFortress(Company company, String fortressCode) throws NotFoundException {
    FortressNode fortress = fortressDao.getFortressByCode(company.getId(), fortressCode);
    if (fortress == null) {
      throw new NotFoundException("Unable to locate the fortress " + fortressCode);
    }
    return fortress;
  }

  @Override
  public String delete(Company company, String fortressCode) {
    FortressNode fortress;
    fortress = findByCode(company, fortressCode);

    if (fortress == null) {
      return "Not Found " + fortressCode;
    }

    return fortressDao.delete(fortress);
  }

  @Override
  public FortressUserNode createFortressUser(Fortress fortress, ContentInputBean inputBean) {
    if (inputBean.getFortressUser() != null) {
      return getFortressUser(fortress, inputBean.getFortressUser(), true);
    }

    return null;
  }

  @Cacheable(value = "geoQuery", key = "#entity.id")
  public String getGeoQuery(Entity entity) {
    DocumentNode documentType = findDocumentType(entity);
    return documentType.getGeoQuery();
  }

  public Segment getDefaultSegment(Fortress fortress) {
    return fortressDao.getDefaultSegment(fortress);
  }

  @Override
  public Segment addSegment(Segment segment) {
    if (segment.getFortress() == null) {
      throw new IllegalArgumentException("Could not associate a fortress with the segment");
    }
//        if ( segment.getCode().equals(FortressSegment.DEFAULT))
//            throw new IllegalArgumentException("Can not use {} as the segment code", segment.getCode());
    return fortressDao.saveSegment(segment);
  }

  @Override
  public Collection<Segment> getSegments(Fortress fortress) {
    return fortressDao.getSegments(fortress);
  }

  @Override
//    @Cacheable (value = "fortressSegment", unless = "#result==null")
  public Segment resolveSegment(Company company, FortressInputBean fortressInput, String segmentName, String timeZone) throws NotFoundException {

    FortressNode fortress;
    Segment segment;

    fortress = findByCode(company, fortressInput.getName());
    if (fortress == null) {
      fortress = findByName(company, fortressInput.getName());
    }
    if (fortress == null) {
      fortress = registerFortress(company, fortressInput, true);
      //resolvedFortresses.put(fortress.getCode(), fortress);
    }
    if (segmentName != null) {
      segment = addSegment(new FortressSegmentNode(fortress, segmentName));
      //resolvedSegments.put(segment.getKey(), segment);
    } else {
      segment = fortress.getDefaultSegment();
    }
    return segment;

  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public FortressNode updateFortress(CompanyNode company, FortressNode existing, FortressInputBean fortressInputBean) {
    existing.setSearchEnabled(fortressInputBean.isSearchEnabled());
    existing.setStoreEnabled(fortressInputBean.isStoreEnabled());
    existing.setTimeZone(fortressInputBean.getTimeZone());
    existing.setName(fortressInputBean.getName());
    existing.setSystem(fortressInputBean.isSystem());
    return fortressDao.update(existing);
  }

  @Override
  public FortressNode findInternalFortress(Company company) {
    String internal = "fd-system-" + company.getId(); // Content models are stored against the internal fortress for the company
    FortressNode systemFortress = findByName(company, internal);
    if (systemFortress == null) {
      systemFortress = save(company, new FortressInputBean(internal)
          .setSystem(true));

    }
    return systemFortress;
  }

  @Override
  public FortressInputBean createDefaultFortressInput() {
    FortressInputBean fib = new FortressInputBean("");
    fib.setStoreEnabled(engineConfig.storeEnabled());
    fib.setSearchEnabled(engineConfig.isSearchEnabled());

    return fib;

  }

  @Override
    /*
      Returns the implementing class of an EntityTagFinder so you can provide runtime node paths
     */
  public EntityTag.TAG_STRUCTURE getTagStructureFinder(Entity entity) {
    EntityTagFinder tagFinder = tagFinders.get(entity.getId());
    if (tagFinder == null) {
      DocumentNode documentType = findDocumentType(entity);
      if (documentType == null || documentType.getTagStructure() == null || documentType.getTagStructure() == EntityTag.TAG_STRUCTURE.DEFAULT) {
        return EntityTag.TAG_STRUCTURE.DEFAULT; // The docType *should* exist
      }
      if (documentType.getTagStructure() == EntityTag.TAG_STRUCTURE.TAXONOMY) {
        return EntityTag.TAG_STRUCTURE.TAXONOMY;
      }
    }
    return EntityTag.TAG_STRUCTURE.DEFAULT;
  }

  public DocumentNode findDocumentType(Entity entity) {
    FortressNode f = (FortressNode) entity.getFortress();

    if (f.getCompany() == null) {
      f = getFortress(f.getId());
    }

    String docType = entity.getType();
    return conceptDao.findDocumentType(f, docType, false);

  }

}
