/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.query.service;

import org.flockdata.data.Company;
import org.flockdata.engine.data.dao.MatrixDao;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.MatrixInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query support for visualization frameworks
 *
 * @author mholdsworth
 * @tag Matrix, Query
 * @since 5/04/2014
 */
@Service
@Transactional
public class MatrixService {
  private final MatrixDao matrixDao;

  @Autowired
  public MatrixService(MatrixDao matrixDao) {
    this.matrixDao = matrixDao;
  }

  public MatrixResults getMatrix(Company company, MatrixInputBean matrixInput) throws FlockException {
    return matrixDao.buildMatrix(company, matrixInput);
  }
}
