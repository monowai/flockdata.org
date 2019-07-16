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

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.Alias;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.SearchResults;
import org.flockdata.search.TagSearchChange;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.CompanyInputBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 2/05/2015
 */

@RunWith(SpringRunner.class)
public class TestQueryDao extends ESBase {
    @Test
    public void entityQueryFromQueryParams() throws Exception {
        Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

        String fortress = "querydao";
        String company = "company";
        String doc = "doc";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, doc, "theCodeToFind", "2015");

        EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);

        SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
        Thread.sleep(1000);
        assertNotNull(searchResults);

        // COde is a term Query
        QueryParams queryParams = new QueryParams()
            .setIndex(searchConfig.getIndexManager().toIndex(entity))
            .setTypes(entity.getType())
            .setCode(entity.getCode());

        EsSearchRequestResult result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrPropertyWithValue("totalHits", 1L)
            .hasFieldOrProperty("json")
        ;

        // User defined Term query
        queryParams = new QueryParams()
            .setIndex(searchConfig.getIndexManager().toIndex(entity))
            .setTypes(entity.getType())
            .addTerm("fortress", entity.getSegment().getFortress().getCode());

        result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrPropertyWithValue("totalHits", 1L)
            .hasFieldOrProperty("json");

        // MatchAll query
        queryParams = new QueryParams("*")
            .setIndex(searchConfig.getIndexManager().toIndex(entity))
            .setSegment(entity.getSegment().getCode())
        ;

        result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrPropertyWithValue("totalHits", 1L)
            .hasFieldOrProperty("json")
        ;

        // MatchAll query within a segment
        queryParams = new QueryParams("*")
            .setIndex(searchConfig.getIndexManager().toIndex(entity))
        ;


        result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrProperty("json")
            .hasFieldOrPropertyWithValue("totalHits", 1L)
        ;

        // Boost Query
        queryParams = new QueryParams(entity.getCode())
            .setMatchAll(true)
            .setIndex(searchConfig.getIndexManager().toIndex(entity))
        ;

        result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrProperty("json")
            .hasFieldOrPropertyWithValue("totalHits", 1L)
        ;

    }

    @Test
    public void findTagByWildCard() throws Exception {
        Company company = new CompanyInputBean("findTagByWildCard");

        TagInputBean tagInputBean = new TagInputBean("simpleCode", "SimpleLabel");
        tagInputBean.setName("A Name to Find");
        tagInputBean.setProperty("user property", "UDFValue");

        Tag tag = MockDataFactory.getTag(tagInputBean);

        String key = TagHelper.parseKey(tagInputBean.getCode() + "Alias");
        Alias alias = MockDataFactory.getAlias(tagInputBean.getLabel(), new AliasInputBean(tagInputBean.getCode() + "Alias", "someAliasDescription"), key, tag);
        Set<Alias> aliasSet = new HashSet<>();
        aliasSet.add(alias);
        when(tag.getAliases()).thenReturn(aliasSet);

        String indexName = searchConfig.getIndexManager().getTagIndexRoot(company, tag);
        org.assertj.core.api.Assertions.assertThat(indexName)
            .isNotNull()
            .contains(".tags")
        ;

        deleteEsIndex(indexName);

        TagSearchChange tagSearchChange = new TagSearchChange(indexName, tag);

        indexMappingService.ensureIndexMapping(tagSearchChange);

        tagWriter.handle(tagSearchChange);
        Thread.sleep(1000);

        QueryParams queryParams = new QueryParams(tag.getName())
            .setIndex(searchConfig.getIndexManager().getTagIndexRoot(company, tag));

        EsSearchRequestResult result = queryServiceEs.doParametrizedQuery(queryParams);
        org.assertj.core.api.Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("fdSearchError", null)
            .hasFieldOrProperty("json")
            .hasFieldOrPropertyWithValue("totalHits", 1L);

    }
}
