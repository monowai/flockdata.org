package com.auditbucket.engine.service;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    public Map<String, Map<String, Long>> getMatrix(Company company, String metaLabel){
        return matrixDao.getMatrix(company, metaLabel);
    }

}
