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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.flockdata.batch.FdAbstractSqlStep;
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.batch.resources.FdTagProcessor;
import org.flockdata.batch.resources.FdTagWriter;
import org.flockdata.registration.TagInputBean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Simple test job t
 *
 * @author mholdsworth
 * @since 28/01/2016
 */
@Configuration
@EnableBatchProcessing
@Profile( {"fd-batch-dev", "dev"})
public class SqlTagStep extends FdAbstractSqlStep {

    @Autowired
    FdRowMapper fdRowMapper;


    public String getStepName() {
        return "olympic.athlete.as.tag";
    }

    @Bean
    public Job runEntityQuery(JobBuilderFactory jobs, @Qualifier("testReadTagFromSQL") Step s1, JobExecutionListener listener) {
        return jobs.get(getStepName())
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(s1)
            .end()
            .build();
    }

    @Bean
    public Step testReadTagFromSQL(ItemReader<Map<String, Object>> tagItemReader, FdTagWriter fdTagWriter, StepBuilderFactory stepBuilderFactory,
                                   FdTagProcessor fdTagProcessor) {

        return stepBuilderFactory.get(getStepName())
            .<Map<String, Object>, TagInputBean>chunk(10)
            .reader(tagItemReader)
            .processor(fdTagProcessor)
            .writer(fdTagWriter)
            .build();
    }

    @Bean
    public ItemReader tagItemReader() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        return getItemReader();
    }
}
