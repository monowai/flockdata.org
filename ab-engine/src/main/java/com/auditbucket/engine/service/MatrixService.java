package com.auditbucket.engine.service;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.query.MatrixResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Query support for visualization frameworks
 *
 * User: mike
 * Date: 5/04/14
 * Time: 9:09 AM
 */
@Service
@Transactional
public class MatrixService {
    @Autowired
    MatrixDao matrixDao;
    public Collection<MatrixResult> getMatrix(Company company, String metaLabel){
        return matrixDao.getMatrix(company, metaLabel);
    }

}
