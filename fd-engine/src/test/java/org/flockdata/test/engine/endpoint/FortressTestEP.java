/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.engine.endpoint;

import junit.framework.TestCase;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.functional.WacBase;
import org.junit.Test;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;

/**
 * Created by mike on 16/02/15.
 */
@WebAppConfiguration
public class FortressTestEP extends WacBase {


    /**
     * Create a collection of DocumentTypeInputBeans for a fortress over the endpoint
     *
     * @throws Exception
     */
    @Test
    public void get_FortressSegments() throws Exception {
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("make_DocTypes", "mike");

        Fortress fortress = createFortress(su);

        login(mike_admin, "123");


        Collection<FortressSegment> segments = getSegments(fortress.getName());
        assertEquals(1, segments.size());

        TestCase.assertTrue("Default segment not found", segments.iterator().next().getCode().equals("Default"));


    }


}
