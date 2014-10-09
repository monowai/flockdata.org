package com.auditbucket.profile.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:45 PM
 */
public interface ImportProfileService {

    public ProfileConfiguration get(Fortress fortress, DocumentType documentType ) throws FlockException ;

    public void save(Fortress fortress, DocumentType documentType, ProfileConfiguration importProfile) throws FlockException;

    public void process(Company company, Fortress fortressCode, DocumentType documentName, String pathToBatch) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException;

    public void process(Company company, String fortressCode, String documentCode, String file) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException;
}
