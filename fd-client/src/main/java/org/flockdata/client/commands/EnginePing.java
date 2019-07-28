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

package org.flockdata.client.commands;

import org.flockdata.transform.FdIoInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author mike
 * @tag
 * @since 13/07/17
 */
@Component
@Qualifier("enginePing")
public class EnginePing extends Ping {

  public EnginePing(FdIoInterface fdIoInterface) {
    super(fdIoInterface);
  }

  @Override
  public String getPath() {
    return "/api/ping/";
  }

  public void setApi(String api) {
    this.api = api;
  }
}
