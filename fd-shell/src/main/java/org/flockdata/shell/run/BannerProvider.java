/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.shell.run;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author mike
 * @tag
 * @since 16/07/17
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)

public class BannerProvider {

  public String getBanner() {
    return "banner";
//        return OsUtils.LINE_SEPARATOR +
//                "=======================================" +
//                OsUtils.LINE_SEPARATOR +
//                "*          Flockdata Shell            *" +
//                OsUtils.LINE_SEPARATOR +
//                "=======================================" +
//                OsUtils.LINE_SEPARATOR +
//                "Version:" +
//                this.getVersion();
  }


  public String getWelcomeMessage() {
    return "Welcome to FlockData CLI";
  }

  public String getProviderName() {
    return "FlockData Banner";
  }

//    @Override
//    public void printBanner(Environment environment, Class<?> aClass, PrintStream printStream) {
//        return "";
//    }
}