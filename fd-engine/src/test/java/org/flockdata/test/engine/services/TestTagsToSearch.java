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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 29/05/2016
 */
public class TestTagsToSearch extends EngineBase {
    @Test
    public void searchTags() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("searchTags", mike_admin);

        Collection<TagInputBean> tagInputs = new ArrayList<>();
        TagInputBean deliveryPoint = new TagInputBean("7md", "Street").setName("7 Manor Drive");
        AliasInputBean dpAlias = new AliasInputBean("7mdxxx", "Street Alias");
        deliveryPoint.addAlias(dpAlias);
        tagInputs.add(deliveryPoint);

        Collection<FdTagResultBean> tagResults = mediationFacade.createTags(su.getCompany(), tagInputs);
        assertEquals(true, searchService.makeTagsSearchable(su.getCompany(), tagResults));

//        tagService.findTag(su.getCompany(), deliveryPoint.getLabel(), null, dpAlias.getCode());


    }
}
