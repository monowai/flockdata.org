package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.repo.neo4j.model.CompanyUser;
import org.springframework.data.neo4j.repository.GraphRepository;


public interface CompanyUserRepository extends GraphRepository<CompanyUser> {


}
