package com.auditbucket.engine;

import com.auditbucket.geography.service.GeographyService;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.service.MediationFacade;
import com.auditbucket.transform.FdReader;
import com.auditbucket.transform.FdWriter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 8:47 AM
 */
@Service
public class FdServerWriter implements FdWriter, FdReader {

    @Autowired
    GeographyService geoService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    // ToDo: Yes this is useless - fix me!
    final String lock = "countryLock";
    private Map<String,Tag> countries = new HashMap<>();

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdServerWriter.class);

    @Override
    public SystemUserResultBean me() {
        return new SystemUserResultBean(securityHelper.getSysUser(true));
    }

    @Override
    public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
        return null;
    }

    @Override
    public String flushEntities(Company company, List<EntityInputBean> entityBatch, boolean async) throws FlockException {
        try {
            if ( company == null )
                company = securityHelper.getCompany();
            if ( async )
                mediationFacade.trackEntitiesAsync(company, entityBatch);
            else
                mediationFacade.trackEntities(company, entityBatch);
            return "ok";
        } catch (InterruptedException e) {
            throw new FlockException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new FlockException("Execution Problem", e);
        } catch (IOException e) {
            throw new FlockException("IO Exception", e);
        }
    }

    @Override
    public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        return 0;
    }

    @Override
    public boolean isSimulateOnly() {
        return false;
    }

    @Override
    public Collection<Tag> getCountries() throws FlockException {

        return geoService.findCountries(securityHelper.getCompany());
    }

    @Override
    public String resolveCountryISOFromName(String name) throws FlockException {
        // 2 char country? it's already ISO
        if (name.length() == 2)
            return name;

        if (countries.isEmpty() ) {
            synchronized (lock) {
                if ( countries.isEmpty()) {
                    Collection<Tag> results = getCountries();

                    for (Tag next : results) {
                        countries.put(next.getName().toLowerCase(), next);
                    }
                }
            }
        }
        Tag tag = countries.get(name.toLowerCase());
        if (tag == null) {
            logger.error("Unable to resolve country name [{}]", name);
            return null;
        }
        return tag.getCode();

    }

    @Override
    public String resolve(String type, Map<String, Object> args) {
        return null;
    }
}
