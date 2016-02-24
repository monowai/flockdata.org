package org.flockdata.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Runs an SQL query and writes the results to FlockData
 *
 * Created by mike on 28/01/16.
 */

@SpringBootApplication
@ComponentScan(value="org.flockdata.batch")
@PropertySource(value = {"classpath:fd-batch.properties", "file:${fd.config}"}, ignoreResourceNotFound = true)
public class Processor {
    public static void main(String[] args) {
        SpringApplication.run(Processor.class, args);
    }


}
