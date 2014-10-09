package com.auditbucket.engine.service;

import com.auditbucket.engine.FdServerWriter;
import com.auditbucket.engine.repo.neo4j.dao.ProfileDao;
import com.auditbucket.engine.repo.neo4j.model.ProfileNode;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.profile.ImportProfile;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.service.SchemaService;
import com.auditbucket.transform.FileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:43 PM
 */
@Service
public class ProfileServiceNeo4j implements ImportProfileService {

    @Autowired
    ProfileDao profileDao;

    @Autowired
    FortressService fortressService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    FdServerWriter fdServerWriter;

    static final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileConfiguration get(Fortress fortress, DocumentType documentType) throws FlockException {
        ProfileNode profile = profileDao.find(fortress, documentType);

        if (profile == null)
            throw new NotFoundException(String.format("Unable to locate and import profile for [%s], [%s]", fortress.getCode(), documentType.getCode()));
        //return profile;
        String json = profile.getContent();
        try {
            ImportProfile iProfile=  objectMapper.readValue(json, ImportProfile.class);
            iProfile.setFortress(fortress.getName());
            iProfile.setDocumentType(documentType.getName());
            return iProfile;
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%d}", profile.getId()), e);
        }
    }

    public void save(Fortress fortress, DocumentType documentType, ProfileConfiguration profileConfig) throws FlockException {
        //objectMapper.
        ProfileNode profile = profileDao.find(fortress, documentType);
        if (profile == null) {
            profile = new ProfileNode(fortress, documentType);
        }
        try {
            profile.setContent(objectMapper.writeValueAsString(profileConfig));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new FlockException("Json error", e);
        }

        // ToDo: Track the change against a FlockData system account
        profileDao.save(profile);
    }

    public void process(Company company, Fortress fortress, DocumentType documentCode, String pathToBatch) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        ProfileConfiguration profile = get(fortress, documentCode);
        FileProcessor fileProcessor = new FileProcessor(fdServerWriter);
        fileProcessor.processFile(profile,pathToBatch, 0, fdServerWriter) ;

    }

    @Override
    public void process(Company company, String fortressCode, String documentCode, String file) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        DocumentType documentType = schemaService.resolveDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type ");
        process(company, fortress, documentType, file);
    }


}
