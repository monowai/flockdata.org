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

package org.flockdata.test.functional;

import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 5:03 PM
 */

public class TestAttachments extends EngineBase {
    @Test
    public void duplicate_sameContentIgnored() throws Exception{
        SystemUser su = registerSystemUser("duplicate_sameContentIgnored", mike_admin);
        FortressInputBean f = new FortressInputBean("attachmentFun", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), f);

        EntityInputBean entity = new EntityInputBean(fortress.getName(), "zippy", "blag", new DateTime(), "ABC");
        ContentInputBean content = new ContentInputBean("zippy", new DateTime());
        content.setAttachment(Helper.getPdfDoc(), "PdF", "testing.pdf");
        entity.setContent(content);
        TrackResultBean trackResult = mediationFacade.trackEntity(fortress, entity);
        assertFalse("This should have been the first entity logged", trackResult.entityExists());

        // Update without changing the content
        trackResult = mediationFacade.trackEntity(fortress, entity);
        assertTrue("Tracked the same file, so should have been ignored",trackResult.entityExists());
    }
}
