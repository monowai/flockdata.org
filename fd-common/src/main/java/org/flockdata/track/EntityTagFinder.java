/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.track;

import org.flockdata.data.EntityTag;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Classes that implement this can find and massage the EntityTag objects that will be
 * sent for indexing in fd-search
 *
 * @author mholdsworth
 * @since 20/08/2015
 */
public interface EntityTagFinder {

  Iterable<EntityTag> getEntityTags(TrackResultBean trackResultBean);

  EntityTag.TAG_STRUCTURE getTagStructure();

}
