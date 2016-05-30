/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
