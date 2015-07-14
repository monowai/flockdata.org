/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.track.model;

import org.flockdata.registration.model.Fortress;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:06 AM
 */
public interface DocumentType {

    public String getName();

    public void setFortress(Fortress fortress);

    Long getId();

    String getCode();

    String getCompanyKey();

    void add(Concept concept);

    Collection<Concept> getConcepts();

    Fortress getFortress();
}
