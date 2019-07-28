package org.flockdata.test.neo.integ;

import static org.assertj.core.api.Assertions.assertThat;

import org.flockdata.data.Company;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.dao.CompanyRepo;
import org.flockdata.graph.model.CompanyNode;
import org.flockdata.integration.Base64;
import org.flockdata.integration.KeyGenService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mikeh
 * @since 10/06/18
 */
@ActiveProfiles( {"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    DriverManager.class,
    Base64.class,
    KeyGenService.class,
    CompanyRepo.class}
)

public class TestCompany extends BaseNeo {

  @Autowired
  CompanyRepo companyRepo;

  @Autowired
  KeyGenService keyGenService;


  @Test
  public void testCreateAndFind() {
    Company myCompany = CompanyNode.builder()
        .name("myName")
        .code("myCode")
        .apiKey(keyGenService.getUniqueKey())
        .build();
    Company created = companyRepo.create(myCompany);
    assertThat(created).isNotNull().hasFieldOrProperty("id");
    Company found = companyRepo.findByCode(myCompany);
    assertThat(found).isNotNull().hasFieldOrProperty("id");
  }
}
