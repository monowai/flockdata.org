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
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.FortressSegment;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collection;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

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

        FortressResultBean fortress = makeFortress(mike(), "make_DocTypes");

        Collection<FortressSegment> segments = getSegments(mike(), fortress.getName());
        assertEquals(1, segments.size());

        TestCase.assertTrue("Default segment not found", segments.iterator().next().getCode().equals("Default"));

    }

    @Test
    public void update_Fortress() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "update_Fortress");
        FortressInputBean update = new FortressInputBean(fortress.getName());

        update.setSearchEnabled(true);
        update.setStoreEnabled(true);
        update.setTimeZone(TimeZone.getTimeZone("PST").getID());
        update.setName("A new name");
        update.setSystem(true);

        FortressResultBean updated = updateFortress(mike(), fortress.getCode(), update, MockMvcResultMatchers.status().isOk());
        assertNotNull(updated);
        assertEquals("Search active did not change", update.getSearchEnabled().booleanValue(), updated.getSearchEnabled());
        assertEquals("Store active did not change", update.getStoreEnabled().booleanValue(), updated.isStoreEnabled());
        assertEquals(update.getName(), updated.getName());


        exception.expect(NotFoundException.class);
        updateFortress(mike(), "doesNotExist", update, MockMvcResultMatchers.status().isNotFound());

        updateFortress(sally(), fortress.getCode(), update, MockMvcResultMatchers.status().isNotFound());
        // Harry is not authorised
        updateFortress(harry(), fortress.getCode(), update, MockMvcResultMatchers.status().isOk());


    }

    @Test
    public void cant_updateFortressThatUserDoesntBelongTo() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "cant_updateFortressThatUserDoesntBelongTo");

        FortressInputBean update = new FortressInputBean(fortress.getName());

        update.setSearchEnabled(true);
        update.setStoreEnabled(true);
        update.setTimeZone(TimeZone.getTimeZone("PST").getID());
        update.setName("A new name");
        update.setSystem(true);

        // Sally works for a different company
        exception.expect(NotFoundException.class);
        updateFortress(sally(), fortress.getCode(), update, MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    public void cant_updateFortressUnlessUserIsAdmin() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "cant_updateFortressUnlessUserIsAdmin");

        FortressInputBean update = new FortressInputBean(fortress.getName());

        update.setSearchEnabled(true);
        update.setStoreEnabled(true);
        update.setTimeZone(TimeZone.getTimeZone("PST").getID());
        update.setName("A new name");
        update.setSystem(true);

        // Sally works for a different company
        exception.expect(AccessDeniedException.class);
        updateFortress(harry(), fortress.getCode(), update, MockMvcResultMatchers.status().isUnauthorized());

    }

    @Test
    public void null_FortressCode() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "null_FortressCode");

        FortressInputBean update = new FortressInputBean(null);

        update.setSearchEnabled(true);
        update.setStoreEnabled(true);
        update.setTimeZone(TimeZone.getTimeZone("NZST").getID());
        update.setName("fName"); // Setting the name should set the code if the code is null
        update.setSystem(true);

        makeFortress(mike(), update);

    }

    @Test
    public void system_Fortress() throws Exception {
        Collection<FortressResultBean> fortresses = getFortresses(mike());

        // In case other tests have left fortresses hanging around, lets get our base count
        int fortressCount = fortresses.size();
        logger.debug("existing fortress count {}", fortressCount);

        makeFortress(mike(), new FortressInputBean("Regular",true));

        FortressInputBean systemFortress = new FortressInputBean("TestSystem",true);
        systemFortress.setSystem(true);
        FortressResultBean fortress = makeFortress(mike(),systemFortress);
        assertTrue ( "Was not created as a system fortress", fortress.isSystem());
        assertTrue ( "system index prefix was not set ["+fortress.getIndexName()+"]", fortress.getIndexName().startsWith(".testfd."));
        fortresses = getFortresses(mike());
        assertEquals("System fortress should not have been returned", fortressCount+1, fortresses.size());
    }
}
