/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.test.integration.matchers;

import org.flockdata.client.commands.CommandResponse;
import org.flockdata.client.commands.TagsGet;
import org.flockdata.registration.TagResultBean;
import org.flockdata.transform.FdIoInterface;

/**
 * @author mholdsworth
 * @since 23/04/2016
 */
public class TagCountReady implements ReadyMatcher {

  private TagsGet tags;
  private int waitFor;
  private CommandResponse<TagResultBean[]> response;
  private String tagLabel;

  public TagCountReady(TagsGet tagGet, int waitFor, String tagLabel) {
    this.waitFor = waitFor;
    this.tags = tagGet;
    this.tagLabel = tagLabel;
  }

  @Override
  public boolean isReady(FdIoInterface fdIoInterface) {
    response = tags.exec(tagLabel);
    return response.getResult() != null && response.getResult().length >= waitFor;
  }

  @Override
  public CommandResponse<TagResultBean[]> getResponse() {
    return response;
  }
}
