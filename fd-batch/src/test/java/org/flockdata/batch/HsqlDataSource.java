package org.flockdata.batch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by mike on 28/01/16.
 */
@Configuration
@Profile("dev")
public class HsqlDataSource {
    @Autowired
    BatchConfig batchConfig;

    @Bean
    @Profile("dev")
    @Primary
    public DataSource dataSource() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .setName("testDb")
                .build();
    }



}
