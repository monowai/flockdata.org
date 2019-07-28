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

import org.flockdata.data.EntityTag;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Encapsulates the functionality to return default entity tags
 *
 * @author mholdsworth
 * @since 24/08/2015
 */
@Repository
public class DefaultTagFinder implements EntityTagFinder {

  final EntityTagService entityTagService;

  @Autowired
  public DefaultTagFinder(EntityTagService entityTagService) {
    this.entityTagService = entityTagService;
  }

  @Override
  public Iterable<EntityTag> getEntityTags(TrackResultBean trackResultBean) {
    if (trackResultBean.getEntity().getId() == null) {
      return trackResultBean.getTags();  // non-persistent entity in the graph, but the trackResult has EntityTags
    } else {
      return entityTagService.findEntityTagsWithGeo(trackResultBean.getEntity());
    }
  }

  @Override
  public EntityTag.TAG_STRUCTURE getTagStructure() {
    return EntityTag.TAG_STRUCTURE.DEFAULT;
  }
}
