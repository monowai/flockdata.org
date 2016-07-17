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
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 29/02/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({IndexManager.class, TestIndexManager.class})
public class TestIndexManager {

    @Autowired
    IndexManager indexManager;
    @Test
    public void testIM() throws Exception{
        Company company = new Company("comp");
        FortressInputBean fortressInput = new FortressInputBean("fort");
        Fortress fortress = new Fortress(fortressInput, company);
        TestCase.assertEquals("overriding the default search prefix is failing", "blah.", indexManager.getPrefix());
        TestCase.assertEquals(indexManager.getPrefix() + company.getCode()+"."+fortress.getCode(),  indexManager.getIndexRoot(fortress));
    }

    // Spring does not support loading properites from Yaml, so we do it this way
    @Bean
    public static PropertySourcesPlaceholderConfigurer setSource() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml;
        yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("/application_dev.yml"));
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }

    @Test
    public void json_QueryParams () throws Exception {
        QueryParams queryParams = new QueryParams("*");
        String json = JsonUtils.toJson(queryParams);
        assertNotNull(json);

        QueryParams qp = JsonUtils.toObject(json.getBytes(), QueryParams.class);
        assertNotNull (qp);
        assertEquals(queryParams.getSearchText(), qp.getSearchText());
    }

    @Test
    public void json_TagResult () throws Exception {
        TagInputBean tagInputBean = new TagInputBean("abc","label");
        TagResultBean tagResult = new TagResultBean( tagInputBean);
        String json = JsonUtils.toJson(tagResult);
        assertNotNull(json);

        TagResultBean tr = JsonUtils.toObject(json.getBytes(), TagResultBean.class);
        assertNotNull (tr);
        assertEquals(tagResult.getCode(), tr.getCode());
    }

}
