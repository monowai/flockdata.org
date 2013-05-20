package com.auditbucket.registration.service;

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.repo.neo4j.dao.RegistrationDaoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemUserService {

    @Autowired
    RegistrationDaoImpl regDao;

    public ISystemUser findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return regDao.findByPropertyValue("name", name.toLowerCase());
    }

    public IFortressUser findBy(String userName, String fortressName, String fortressUser) {
        return regDao.getFortressUser(userName, fortressName, fortressUser);
    }

    @Transactional
    public ISystemUser save(RegistrationBean regBean) {
        return regDao.save(regBean.getCompany(), regBean.getName(), regBean.getPassword());
    }
}
