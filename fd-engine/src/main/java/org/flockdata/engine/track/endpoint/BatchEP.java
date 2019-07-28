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

package org.flockdata.engine.track.endpoint;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.track.service.BatchService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.ContentValidationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Batch, EndPoint, Track
 * @since 7/10/2014
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/batch")
public class BatchEP {
  private final BatchService batchService;

  @Autowired
  public BatchEP(BatchService batchService) {
    this.batchService = batchService;
  }

  @RequestMapping(value = "/{fortress}/{document}/import", consumes = "application/json", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  public void track(
      HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName, @RequestBody Map file) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Object filename = file.get("file");
    if (filename == null) {
      throw new NotFoundException("No file to process");
    }

    batchService.validateArguments(company, fortressCode, documentName, filename.toString());
    boolean async = false;
    Object value = file.get("async");
    if (value != null) {
      async = Boolean.parseBoolean(value.toString());
    }

    if (async) {
      batchService.processAsync(company, fortressCode, documentName, filename.toString());
    } else {
      batchService.process(company, fortressCode, documentName, filename.toString(), async);
    }
  }


  @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
  public ContentValidationRequest trackData(@RequestBody ContentValidationRequest validationRequest,
                                            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return batchService.process(company, validationRequest);

  }
}
