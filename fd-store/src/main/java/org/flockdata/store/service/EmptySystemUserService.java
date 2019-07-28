package org.flockdata.store.service;

import org.flockdata.authentication.SystemUserService;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.springframework.stereotype.Service;

/**
 * @author mikeh
 * @since 2018-11-15
 */
@Service
public class EmptySystemUserService implements SystemUserService {
  @Override
  public SystemUser findByLogin(String userEmail) {
    return null;
  }

  @Override
  public SystemUser findByApiKey(String apiKey) {
    return null;
  }

  @Override
  public SystemUser save(Company company, RegistrationBean regBean) {
    return null;
  }

  @Override
  public void save(SystemUser systemUser) {

  }
}
