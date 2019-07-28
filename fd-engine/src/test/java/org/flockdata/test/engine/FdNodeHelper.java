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

import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;

/**
 * Simplifies the creation of node objects that will be persisted in fd-engine
 *
 * @author mike
 * @tag Test, Entity
 * @since 4/01/17
 */
public class FdNodeHelper {
  public static Entity getEntity(String comp, String fort, String userName, String docType) throws FlockException {
    String code = new DateTime().toString();
    return getEntity(comp, fort, userName, docType, code);
  }

  public static Entity getEntity(String comp, String fort, String userName, String docType, String code) throws FlockException {
    // These are the minimum objects necessary to create Entity data

    CompanyNode mockCompany = CompanyNode.builder().name(comp).build();
    mockCompany.setName(comp);

    FortressInputBean fib = new FortressInputBean(fort, false);

    FortressNode fortress = new FortressNode(fib, mockCompany);

    DateTime now = new DateTime();
    EntityInputBean entityInput = new EntityInputBean(fortress, userName, docType, now, code);

    Document doc = new DocumentNode(fortress, docType);
    return new EntityNode(Long.toString(System.currentTimeMillis()), fortress.getDefaultSegment(), entityInput, doc);

  }
}
