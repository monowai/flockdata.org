package org.flockdata.test.batch;

import org.flockdata.batch.FdAbstractSqlJob;
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.track.bean.EntityInputBean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Simple test job t
 *
 * Created by mike on 28/01/16.
 */
@Configuration
@EnableBatchProcessing
public class SqlQueryJob extends FdAbstractSqlJob {

    @Autowired
    FdRowMapper fdRowMapper;


    public String getStepName() {
        return "olympic.athlete";
    }

        @Bean
    public Job runSQLQuery(JobBuilderFactory jobs, @Qualifier("readSql") Step s1, JobExecutionListener listener) {
        return jobs.get(getStepName())
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(s1)
                .end()
                .build();
    }

    @Bean
    public Step readSql(StepBuilderFactory stepBuilderFactory, ItemReader<Map<String, Object>> reader,
                        ItemWriter<EntityInputBean> writer, ItemProcessor<Map<String, Object>, EntityInputBean> processor) {

        return stepBuilderFactory.get(getStepName())
                .<Map<String, Object>, EntityInputBean> chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ItemReader itemReader() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        return getItemReader();
    }
}
