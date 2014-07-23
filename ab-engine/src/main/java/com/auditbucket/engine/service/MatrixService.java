package com.auditbucket.engine.service;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public MatrixResults getMatrix(Company company, MatrixInputBean matrixInput) {
        return matrixDao.getMatrix(company, matrixInput);
    }
}
