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

import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.registration.TagInputBean;

import java.util.Collection;

/**
 * @author mholdsworth
 * @since 5/09/2014
 */
public interface SchemaService {

    Boolean ensureSystemIndexes(CompanyNode company);

    /**
     * Deletes the majority of the structural associations between a fortress and data in FlockData
     * Does not delete the DocumentType
     *
     * You probably want to be calling adminService.purge() which in turn calls this
     *
     * @param fortress
     */
    void purge(Fortress fortress);

    Boolean ensureUniqueIndexes(Collection<TagInputBean> tagInputs);

}
