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

import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import org.flockdata.data.Alias;
import org.flockdata.data.Company;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.TagSearchChange;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.CompanyInputBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 16/05/2016
 */
@RunWith(SpringRunner.class)
public class TestIndexTagChanges extends ESBase {
    @Test
    public void testSimpleTagIndexes() throws Exception {
        Company company = new CompanyInputBean("testTagCompany");

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
            .endsWith("." + searchConfig.getIndexManager().parseType(tagInputBean.getLabel()))
        ;

        deleteEsIndex(indexName);

        TagSearchChange tagSearchChange = new TagSearchChange(indexName, tag);

        indexMappingService.ensureIndexMapping(tagSearchChange);

        tagWriter.handle(tagSearchChange);
        Thread.sleep(1000);
        // Find by code
        doQuery(indexName, tag.getLabel().toLowerCase(), tag.getCode(), 1);
        // By Name
        doQuery(indexName, tag.getLabel().toLowerCase(), tag.getName(), 1);
        // Alias
        doQuery(indexName, tag.getLabel().toLowerCase(), alias.getName(), 1);
    }
}
