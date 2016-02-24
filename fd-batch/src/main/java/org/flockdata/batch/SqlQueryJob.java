package org.flockdata.batch;

import org.flockdata.batch.resources.FdBatchResources;
import org.flockdata.batch.resources.FdRowMapper;
import org.flockdata.batch.resources.FlockDataItemProcessor;
import org.flockdata.batch.resources.FlockDataItemWriter;
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
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by mike on 28/01/16.
 */
@Configuration
@EnableBatchProcessing
public class SqlQueryJob {
    @Autowired
    FdBatchResources dataSource;

    @Value("${fd.step.name:'runSql'}")
    String stepName;

    public String getStepName() {
        return stepName;
    }

    @Bean
    public Job runSQLQuery(JobBuilderFactory jobs, Step s1, JobExecutionListener listener) {
        return jobs.get("load")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(s1)
                .end()
                .build();
    }

    @Bean
    public Step readSql(StepBuilderFactory stepBuilderFactory, ItemReader<Map<String, Object>> reader,
                        ItemWriter<EntityInputBean> writer, ItemProcessor<Map<String, Object>, EntityInputBean> processor) {

        return stepBuilderFactory.get(stepName)
                .<Map<String, Object>, EntityInputBean> chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ItemProcessor<Map<String, Object>, EntityInputBean> itemProcessor() {
        return new FlockDataItemProcessor();
    }

    @Bean
    public ItemWriter<EntityInputBean> itemWriter() {
        return new FlockDataItemWriter();
    }

    @Bean
    public RowMapper<Map<String, Object>> rowMapper() {
        return new FdRowMapper();
    }
    @Autowired
    BatchConfig batchConfig;

    @Bean
    public ItemReader itemReader() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        JdbcCursorItemReader itemReader = new org.springframework.batch.item.database.JdbcCursorItemReader();
        itemReader.setSql(batchConfig.getSqlQuery());
        itemReader.setDataSource(dataSource.dataSource());
        itemReader.setRowMapper(rowMapper());
        return itemReader;
    }
}
