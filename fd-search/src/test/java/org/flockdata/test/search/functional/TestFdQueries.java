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

package org.flockdata.test.search.functional;

import org.flockdata.model.Entity;
import org.flockdata.search.FdSearch;
import org.flockdata.search.model.*;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.test.engine.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 27/04/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
public class TestFdQueries extends ESBase{

    @Autowired
    QueryServiceEs queryServiceEs;

    @Test
    public void query_EndPoints() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "epFortress";
        String company = "epCompany";
        String doc = "epDocType";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, doc);
        deleteEsIndex(entity);
        EntitySearchChange change = new EntitySearchChange(entity, indexHelper.parseIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);

        deleteEsIndex(entity);
        Thread.sleep(1000);

        SearchResults searchResults = esSearchWriter.createSearchableChange(new EntitySearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(2000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());

        QueryParams qp = new QueryParams(entity.getSegment());
        qp.setCompany(company);
        qp.setSearchText("*");
        // Sanity check - there is only one document in the index
        EsSearchResult queryResult = queryServiceEs.doFdViewSearch(qp);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getMetaKey(), queryResult.getResults().iterator().next().getMetaKey());
        MetaKeyResults metaResults = queryServiceEs.doMetaKeyQuery(qp);
        assertEquals(1, metaResults.getResults().size());
        assertEquals(entity.getMetaKey(), metaResults.getResults().iterator().next());

        // Find with just a fortress
        qp = new QueryParams(entity.getSegment());
        qp.setSearchText("description");
        queryResult = queryServiceEs.doFdViewSearch(qp);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getMetaKey(), queryResult.getResults().iterator().next().getMetaKey());

        qp = new QueryParams().setCompany(company.toLowerCase());
        qp.setSearchText("description");
        queryResult = queryServiceEs.doFdViewSearch(qp);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getMetaKey(), queryResult.getResults().iterator().next().getMetaKey());

        qp = new QueryParams(entity.getSegment());
        qp.setSearchText("-description"); // Ignore description
        queryResult = queryServiceEs.doFdViewSearch(qp);
        assertEquals(0, queryResult.getResults().size());


    }
}
