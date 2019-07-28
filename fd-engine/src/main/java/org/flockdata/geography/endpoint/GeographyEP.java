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

package org.flockdata.geography.endpoint;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.Company;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.FdTagResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Geography related functions
 *
 * @author mholdsworth
 * @tag Geo, Country
 * @since 27/04/2014
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/geo")
public class GeographyEP {

  private final GeographyService geoService;

  @Autowired
  public GeographyEP(GeographyService geoService) {
    this.geoService = geoService;
  }

  @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
  public Collection<FdTagResultBean> findCountries(HttpServletRequest request) throws FlockException {
    Company company = CompanyResolver.resolveCompany(request);
    return geoService.findCountries(company);
  }

}
