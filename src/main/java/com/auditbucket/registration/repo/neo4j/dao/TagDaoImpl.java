package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.TagDaoI;
import com.auditbucket.registration.model.ITag;
import com.auditbucket.registration.repo.neo4j.TagRepository;
import com.auditbucket.registration.repo.neo4j.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:33 PM
 */
@Repository
public class TagDaoImpl implements TagDaoI {

    @Autowired
    TagRepository tagRepo;

    public ITag save(ITag tag) {
        Tag tagToCreate;
        if ((tag instanceof Tag))
            tagToCreate = (Tag) tag;
        else
            tagToCreate = new Tag(tag);

        return tagRepo.save(tagToCreate);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ITag findOne(String tagName, Long id) {
        return tagRepo.findCompanyTag(tagName, id);
    }
}
