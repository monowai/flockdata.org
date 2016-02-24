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

package org.flockdata.test.engine.mvc;

import junit.framework.TestCase;
import org.flockdata.model.FortressSegment;
import org.flockdata.registration.FortressResultBean;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;

/**
 * Created by mike on 16/02/15.
 */
public class TestFortressEP extends MvcBase {


    /**
     * Create a collection of DocumentTypeInputBeans for a fortress over the endpoint
     *
     * @throws Exception
     */
    @Test
    public void get_FortressSegments() throws Exception {

        FortressResultBean fortress = createFortress(mike(), "make_DocTypes");


        Collection<FortressSegment> segments = getSegments(mike(), fortress.getName());
        assertEquals(1, segments.size());

        TestCase.assertTrue("Default segment not found", segments.iterator().next().getCode().equals("Default"));


    }


}