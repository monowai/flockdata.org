package com.auditbucket.dao;

import com.auditbucket.registration.model.Company;
import com.auditbucket.track.query.MatrixResult;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 5/04/14
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MatrixDao {

    Collection<MatrixResult> getMatrix(Company company, String metaLabel);
}
