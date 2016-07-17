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

package org.flockdata.transform;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;

import java.io.IOException;
import java.util.Collection;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 12:18 PM
 */
public interface FdWriter {
    /**
     * Resolve the currently logged in user
     * @return su
     */
    SystemUserResultBean me();

    String writeTags(Collection<TagInputBean> tagInputBeans) throws FlockException;

    String writeEntities(Collection<EntityInputBean> entityBatch) throws FlockException;

    ContentModel getContentModel(ClientConfiguration clientConfiguration, String fileModel) throws IOException;

}
