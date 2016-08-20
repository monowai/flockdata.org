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

package org.flockdata.test.integration;


import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.model.QueryParams;
import org.junit.Test;

/**
 * Created by mike on 29/02/16.
 */
public class TestIndexManager {

    @Test
    public void testIM() throws Exception{
        Company company = new Company("comp");
        FortressInputBean fortressInput = new FortressInputBean("fort");
        Fortress fortress = new Fortress(fortressInput, company);
        IndexManager indexManager = new IndexManager("blah.", true);
        TestCase.assertEquals("overriding the default search prefix is failing", "blah.", indexManager.getPrefix());
        TestCase.assertEquals(indexManager.getPrefix() + company.getCode()+"."+fortress.getCode(),  indexManager.getIndexRoot(fortress));
    }

    @Test
    public void json_QueryParams () throws Exception {
        QueryParams queryParams = new QueryParams("*");
        String json = JsonUtils.toJson(queryParams);
        TestCase.assertNotNull(json);

        QueryParams qp = JsonUtils.toObject(json.getBytes(), QueryParams.class);
        TestCase.assertNotNull (qp);
        TestCase.assertEquals(queryParams.getSearchText(), qp.getSearchText());
    }

    @Test
    public void json_TagResult () throws Exception {
        TagInputBean tagInputBean = new TagInputBean("abc","label");
        TagResultBean tagResult = new TagResultBean( tagInputBean);
        String json = JsonUtils.toJson(tagResult);
        TestCase.assertNotNull(json);

        TagResultBean tr = JsonUtils.toObject(json.getBytes(), TagResultBean.class);
        TestCase.assertNotNull (tr);
        TestCase.assertEquals(tagResult.getCode(), tr.getCode());
    }

}
