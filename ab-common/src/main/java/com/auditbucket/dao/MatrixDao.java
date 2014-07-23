package com.auditbucket.dao;

import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 5/04/14
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MatrixDao {

    MatrixResults getMatrix(Company company, MatrixInputBean inputBean);
}
