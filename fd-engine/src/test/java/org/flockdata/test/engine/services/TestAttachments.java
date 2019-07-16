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

package org.flockdata.test.engine.services;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 17/09/2014
 */

public class TestAttachments extends EngineBase {
    @Test
    public void duplicate_sameContentIgnored() throws Exception {
        SystemUser su = registerSystemUser("duplicate_sameContentIgnored", mike_admin);
        FortressInputBean f = new FortressInputBean("attachmentFun", true);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), f);

        EntityInputBean entity = new EntityInputBean(fortress, "zippy", "blag", new DateTime(), "ABC");
        ContentInputBean content = new ContentInputBean("zippy", new DateTime());
        content.setAttachment(ContentDataHelper.getPdfDoc(), "PdF", "testing.pdf");
        entity.setContent(content);
        TrackResultBean trackResult = mediationFacade.trackEntity(fortress.getDefaultSegment(), entity);
        assertFalse("This should have been the first entity logged", trackResult.entityExists());

        // Update without changing the content
        trackResult = mediationFacade.trackEntity(fortress.getDefaultSegment(), entity);
        assertTrue("Tracked the same file, so should have been ignored", trackResult.entityExists());
    }
}
