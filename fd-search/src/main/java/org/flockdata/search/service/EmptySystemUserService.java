package org.flockdata.search.service;

import org.flockdata.authentication.SystemUserService;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.springframework.stereotype.Service;

/**
 * ToDo: Figure out SecurityHelpers dependency on this and remove it
 *
 * @author mikeh
 * @since 16/06/18
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
