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

package org.flockdata.test.search.functional;

import org.flockdata.data.Entity;
import org.flockdata.search.*;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.test.helper.ContentDataHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mholdsworth
 * @since 27/04/2015
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FdSearch.class)
public class TestFdQueries extends ESBase{

    @Autowired
    QueryServiceEs queryServiceEs;

    @Test
    public void query_EndPoints() throws Exception {
        Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

        String fortress = "query_EndPoints";
        String company = "query_EndPoints";
        String doc = "epDocType";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, doc);
        deleteEsIndex(entity);
        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);

        deleteEsIndex(entity);
        Thread.sleep(1000);

        SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(2000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());

        QueryParams queryParams = new QueryParams(entity.getSegment());
        queryParams.setCompany(company);
        queryParams.setSearchText("*");
        // Sanity check - there is only one document in the index
        EsSearchResult queryResult = queryServiceEs.doFdViewSearch(queryParams);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getKey(), queryResult.getResults().iterator().next().getKey());
        EntityKeyResults metaResults = queryServiceEs.doKeyQuery(queryParams);
        assertEquals(1, metaResults.getResults().size());
        assertEquals(entity.getKey(), metaResults.getResults().iterator().next());

        // Find with just a fortress
        queryParams = new QueryParams(entity.getSegment());
        queryParams.setSearchText("description");
        queryResult = queryServiceEs.doFdViewSearch(queryParams);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getKey(), queryResult.getResults().iterator().next().getKey());

        queryParams = new QueryParams().setCompany(company.toLowerCase());
        queryParams.setSearchText("description");
        queryResult = queryServiceEs.doFdViewSearch(queryParams);
        assertEquals(1, queryResult.getResults().size());
        assertEquals(entity.getKey(), queryResult.getResults().iterator().next().getKey());

        queryParams = new QueryParams(entity.getSegment());
        queryParams.setSearchText("-description"); // Ignore description
        queryResult = queryServiceEs.doFdViewSearch(queryParams);
        assertEquals(0, queryResult.getResults().size());

    }
    @Test
    public void query_EsPassthrough() throws Exception {
        Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

        String fortress = "query_EsPassthrough";
        String company = "query_EsPassthrough";
        String doc = "epDocType";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, doc);
        deleteEsIndex(entity);
        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);

        SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(2000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());

        QueryParams queryParams = new QueryParams(entity.getSegment());
        queryParams.setCompany(company);
        String results = queryServiceEs.doSearch(queryParams);
        assertNotNull("Hmm, not defaulting the query to a match_all?", results);
        assertTrue(results.contains(fortress));
    }
}
