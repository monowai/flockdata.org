package org.flockdata.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by mike on 17/02/16.
 */
@SpringBootApplication(scanBasePackages = {"org.flockdata.store", "org.flockdata.authentication", "org.flockdata.search"})
public class FdStore {
    public static void main(String[] args) {
        SpringApplication.run(FdStore.class, args);
    }
}
