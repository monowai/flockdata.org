/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.mvc;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class TestAPISecurity extends MvcBase {

  @Test
  public void invokeSecureAPIWithoutAPIKey_shouldThrowError()
      throws Exception {
    mvc().perform(MockMvcRequestBuilders
        .get(MvcBase.apiPath + "/fortress/")
        .with(noUser()))

        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  public void invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk()
      throws Exception {
    makeDataAccessProfile("invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk", sally_admin);

    mvc()
        .perform(MockMvcRequestBuilders
            .get(MvcBase.apiPath + "/fortress/")
            .with(sally())
        )

        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
  }

  @Test
  public void invokeSecureAPIWithAPIKeyWithoutLogin_shouldReturnOk() throws Exception {
    String apikey = suMike.getApiKey();
    setSecurityEmpty();

    mvc().perform(
        MockMvcRequestBuilders.get(MvcBase.apiPath + "/fortress/").header("api-key",
            apikey)
            .with(noUser()))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
  }
}
