package org.flockdata.test.neo.integ;

import static org.assertj.core.api.Assertions.assertThat;

import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.SystemUser;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.dao.CompanyRepo;
import org.flockdata.graph.service.RegistrationServiceNeo4j;
import org.flockdata.graph.service.SystemUserServiceNeo4j;
import org.flockdata.integration.Base64;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.services.RegistrationService;
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
    KeyGenService.class,
    Base64.class,
    CompanyRepo.class,
    SystemUserServiceNeo4j.class,
    SecurityHelper.class,
    RegistrationServiceNeo4j.class}
)

public class TestRegistration extends BaseNeo {

  @Autowired
  RegistrationService registrationService;

  @Test
  public void testRegistration() throws Exception {
    RegistrationBean registrationBean = RegistrationBean.builder()
        .companyName("testRegistration")
        .login("test@login.com")
        .build();

    SystemUser created = registrationService.registerSystemUser(registrationBean);
    assertThat(created).isNotNull()
        .hasNoNullFieldsOrProperties();

    SystemUser found = registrationService.getSystemUser(created.getApiKey());

    assertThat(found).isNotNull()
        .hasFieldOrPropertyWithValue("apiKey", created.getApiKey())
        .hasFieldOrPropertyWithValue("id", created.getId());

    SystemUser existing = registrationService.registerSystemUser(registrationBean);

    assertThat(existing).isNotNull()
        .hasFieldOrPropertyWithValue("apiKey", found.getApiKey());

//        assertThat(found).isNotNull().hasFieldOrProperty("id");
  }
}
