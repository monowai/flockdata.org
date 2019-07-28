package org.flockdata.graph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author mikeh
 * @since 10/06/18
 */

@SpringBootApplication(scanBasePackages = {"org.flockdata", "org.flockdata.registration"})
@EnableAutoConfiguration
public class FdGraph {
  public static void main(String[] args) {
    try {
      SpringApplication.run(FdGraph.class, args);
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
    }

  }

}
