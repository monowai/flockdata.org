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

package org.flockdata.test.engine;

import org.flockdata.engine.admin.service.FdStorageProxy;
import org.flockdata.integration.InMemoryRepo;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Simple map based storage proxy for testing. Overrides methods and imports the basic MapRepo from kv-store
 *
 * @author mholdsworth
 * @since 18/02/2016
 */
@Service
@Profile( {"dev"})
public class MapBasedStorageProxy extends FdStorageProxy {
  @Autowired
  private InMemoryRepo inMemoryRepo;

  @Override
  public void write(TrackResultBean trackResult) {
    StoredContent content = new StorageBean(trackResult);
    inMemoryRepo.add(content);
  }

  @Override
  public StoredContent read(LogRequest logRequest) {
    return inMemoryRepo.read(logRequest);
  }

}
