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

package org.flockdata.engine.track.service;

import java.io.IOException;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.ContentValidationRequest;

/**
 * @author mholdsworth
 * @since 24/05/2016
 */
public interface BatchService {
  void process(CompanyNode company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException;

  void processAsync(CompanyNode company, String fortressCode, String documentName, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException;

  int process(Company company, FortressNode fortressCode, Document documentName, String pathToBatch, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException;

  void validateArguments(CompanyNode company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException;


  ContentValidationRequest process(CompanyNode company, ContentValidationRequest validationRequest);
}
