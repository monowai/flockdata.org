package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.FortressDaoI;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.FortressRepository;
import com.auditbucket.registration.repo.neo4j.FortressUserRepository;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 10:29 PM
 */
@Repository
public class FortressDaoImpl implements FortressDaoI {
    @Autowired
    private FortressRepository fortressRepo;
    @Autowired
    private FortressUserRepository fortressUserRepo;

    @Override
    public IFortress save(IFortress fortress) {
        return fortressRepo.save((Fortress) fortress);
    }

    @Override
    public IFortress findByPropertyValue(String name, Object value) {
        return fortressRepo.findByPropertyValue(name, value);
    }

    @Override
    public IFortress findOne(Long id) {
        return fortressRepo.findOne(id);
    }

    @Autowired
    Neo4jTemplate template;

    @Override
    public IFortressUser getFortressUser(Long id, String name) {
        IFortressUser fu = fortressRepo.getFortressUser(id, name);
        if (fu != null)
            template.fetch(fu.getFortress());
        return fu;
    }

    @Override
    public List<IFortress> findFortresses(Long companyID) {

//        TraversalDescription td = Traversal.description()
//                .breadthFirst()
//                .relationships( DynamicRelationshipType.withName("owns"), Direction.OUTGOING )
//                .evaluator( Evaluators.excludeStartPosition() );

        //return fortressRepo.findAllByTraversal(companyID, td );

        return fortressRepo.findCompanyFortresses(companyID);
    }

    @Override
    public IFortressUser findOneUser(Long id) {
        return fortressUserRepo.findOne(id);
    }

    @Override
    public IFortressUser save(IFortressUser fortressUser) {
        return fortressUserRepo.save((FortressUser) fortressUser);
    }


}
