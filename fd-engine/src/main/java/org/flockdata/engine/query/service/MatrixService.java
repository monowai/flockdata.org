/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.query.service;

import org.flockdata.dao.MatrixDao;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
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

    public MatrixResults getMatrix(Company company, MatrixInputBean matrixInput) throws FlockException {
        return matrixDao.buildMatrix(company, matrixInput);
    }
}
