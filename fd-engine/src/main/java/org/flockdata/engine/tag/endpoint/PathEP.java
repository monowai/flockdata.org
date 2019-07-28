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

package org.flockdata.engine.tag.endpoint;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.tag.service.TagPath;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, Tag
 * @since 28/12/2015
 */

@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/path")
public class PathEP {

  @Autowired
  TagPath tagPath;

  @RequestMapping(value = "/{label}/{code}/{length}/{targetLabel}", produces = "application/json", method = RequestMethod.GET)
  public Collection<Map<String, Object>> getConnectedTags(@PathVariable("label") String label, @PathVariable("code") String code,
                                                          HttpServletRequest request, @PathVariable("targetLabel") String targetLabel, @PathVariable("length") Integer length) throws FlockException, UnsupportedEncodingException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return tagPath.getPaths(company, URLDecoder.decode(label, "UTF-8"), URLDecoder.decode(code, "UTF-8"), length, URLDecoder.decode(targetLabel, "UTF-8"));
//        return tagService.findTags(company, label, code, relationship, targetLabel);
  }


}
