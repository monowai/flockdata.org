package com.auditbucket.dao;

import com.auditbucket.registration.model.Company;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 5/04/14
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MatrixDao {

    Map<String, Map<String, Long>> getMatrix(Company company, String metaLabel);
}
