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


import static org.assertj.core.api.Assertions.assertThat;

import junit.framework.TestCase;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.QueryParams;
import org.flockdata.track.bean.FdTagResultBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 29/02/2016
 */
public class TestIndexManager {

    @Test
    public void testIM() throws Exception {
        CompanyNode company = new CompanyNode("comp");
        FortressInputBean fortressInput = new FortressInputBean("fort");
        FortressNode fortress = new FortressNode(fortressInput, company);
        IndexManager indexManager = new IndexManager("blah.", true);
        TestCase.assertEquals("overriding the default search prefix is failing", "blah.", indexManager.getPrefix());
        TestCase.assertEquals(indexManager.getPrefix() + company.getCode() + "." + fortress.getCode(), indexManager.getIndexRoot(fortress));
    }

    @Test
    public void json_QueryParams() throws Exception {
        QueryParams queryParams = new QueryParams("*");
        String json = JsonUtils.toJson(queryParams);
        TestCase.assertNotNull(json);

        QueryParams qp = JsonUtils.toObject(json.getBytes(), QueryParams.class);
        assertThat(qp)
            .isNotNull()
            .hasFieldOrPropertyWithValue("searchText", qp.getSearchText());
    }

    @Test
    public void json_TagResult() throws Exception {
        TagInputBean tagInputBean = new TagInputBean("abc", "label");
        TagResultBean tagResult = new FdTagResultBean(tagInputBean);
        String json = JsonUtils.toJson(tagResult);
        TestCase.assertNotNull(json);

        TagResultBean tr = JsonUtils.toObject(json.getBytes(), TagResultBean.class);
        TestCase.assertNotNull(tr);
        TestCase.assertEquals(tagResult.getCode(), tr.getCode());
    }

}
