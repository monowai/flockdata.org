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

package org.flockdata.integration;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;

import java.util.Collection;
import java.util.List;

/**
 * Functions to read and write data to the service. This may involve buffering data for a
 * batched dispatch. This is the preferred interface to injects into you code to handle
 * reading and writing of entities and tags
 *
 * @tag Rest, Integration, Entity, Tag
 * @author mholdsworth
 * @since 5/03/2016
 * @see FdPayloadWriter
 */
public interface PayloadWriter {

    /**
     * @param tagInputBean payload
     * @param message      used in logging. Name of the process
     * @throws FlockException failure to communicate to the services
     */
    void writeTag(TagInputBean tagInputBean, String message) throws FlockException;

    /**
     *
     * @param tagInputBean   collection of tags
     * @param message      used in logging. Name of the process
     * @throws FlockException failure to communicate to the services
     */
    void writeTags(Collection<TagInputBean> tagInputBean, String message) throws FlockException;

    /**
     *
     * @param entityInputBean payload
     * @throws FlockException failure to communicate to the service
     */
    void writeEntity(EntityInputBean entityInputBean) throws FlockException;

    /**
     *
     * @param entityInputBean payload
     * @param flush           force a flush of any cached data
     * @throws FlockException failure to communicate to the service
     */
    void writeEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException;

    /**
     * Push all payloads to the service
     */
    void flush();

    /**
     * clear down any cached payloads
     */
    void reset();

    /**
     *
     * @return cached entity payloads
     */
    List<EntityInputBean> getEntities();

    /**
     *
     * @return cached tag payloads
     */
    List<TagInputBean> getTags();


}
