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

package org.flockdata.dao;

import org.flockdata.helper.FlockException;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.model.Company;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 5/04/14
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MatrixDao {

    MatrixResults buildMatrix(Company company, MatrixInputBean inputBean) throws FlockException;
}
