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

package org.flockdata.test.helper;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 23/10/15.
 */
public class TestEntityKeyBean {

    @Test
    public void nullCompanyIssues() throws Exception{
        EntityKeyBean ekb = new EntityKeyBean("docTypeName", "fortress", "code");
        String json = JsonUtils.toJson(ekb);
        assertNotNull(json);
        EntityKeyBean otherBean = JsonUtils.toObject(json.getBytes(), EntityKeyBean.class);
        TestCase.assertEquals(ekb.getCode(), otherBean.getCode());
        TestCase.assertEquals(ekb.getFortressName(), otherBean.getFortressName());
        TestCase.assertEquals(ekb.getDocumentType(), otherBean.getDocumentType());
    }
}
