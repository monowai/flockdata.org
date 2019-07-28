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

package org.flockdata.batch;

import java.io.IOException;
import java.sql.SQLException;
import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.integration.ClientConfiguration;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author mholdsworth
 * @since 3/03/2016
 */
public abstract class FdAbstractSqlStep {
  @Autowired
  protected FdBatchResources batchResources;

  @Autowired
  protected BatchConfig batchConfig;

  @Autowired
  protected ClientConfiguration clientConfiguration;

  @Autowired
  protected RowMapper fdRowMapper;

  protected abstract String getStepName();

  protected ItemReader getItemReader() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
    JdbcCursorItemReader itemReader = new JdbcCursorItemReader();
    itemReader.setSql(batchConfig.getStepConfig(getStepName()).getQuery());
    itemReader.setDataSource(batchResources.dataSource());
    itemReader.setRowMapper(fdRowMapper);
    return itemReader;

  }

}