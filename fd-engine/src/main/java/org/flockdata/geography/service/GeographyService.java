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

package org.flockdata.geography.service;

import java.util.Collection;
import org.flockdata.data.Company;
import org.flockdata.engine.tag.service.TagService;
import org.flockdata.track.bean.FdTagResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 27/04/2014
 */
@Service
@Transactional
public class GeographyService {
  private final TagService tagService;

  @Autowired
  public GeographyService(TagService tagService) {
    this.tagService = tagService;
  }

  public Collection<FdTagResultBean> findCountries(Company company) {
    return tagService.findTagResults(company, "Country");

  }
}
