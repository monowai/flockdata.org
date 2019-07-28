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

package org.flockdata.test.unit.batch;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.flockdata.batch.BatchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author mholdsworth
 * @since 28/01/2016
 */
@Configuration
@Profile( {"fd-batch-dev", "dev"})
public class HsqlDataSource {
  @Autowired
  BatchConfig batchConfig;

  @Bean
  @Profile("fd-batch-dev")
  @Primary
  public DataSource dataSource() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.HSQL)
        .setName("testDb")
        .build();
  }


}
