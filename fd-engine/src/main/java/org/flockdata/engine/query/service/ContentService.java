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
import org.flockdata.data.Fortress;
import org.flockdata.engine.integration.search.ContentStructureRequest;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @since 31/08/2016
 */
@Service
public class ContentService {

  private ContentStructureRequest.ContentStructureGateway gateway;

  @Autowired
  void setGateway(ContentStructureRequest.ContentStructureGateway gateway) {
    this.gateway = gateway;
  }

  public ContentStructure getStructure(Company company, Fortress fortress, String docType) {
    QueryParams qp = new QueryParams()
        .setCompany(company.getName())
        .setFortress(fortress.getName())
        .setTypes(docType);
    return gateway.getStructure(qp);
//        return gateway.getStructure(company.getName(), fortress.getName(), docType);

  }
}
