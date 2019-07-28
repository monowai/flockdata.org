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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * @author mike
 * @tag shell
 * @since 13/07/17
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ShellPromptProvider implements PromptProvider {
  @Override
  public AttributedString getPrompt() {
    AttributedStringBuilder builder = new AttributedStringBuilder();

    builder.append("fd-shell$ ", AttributedStyle.BOLD);
    //return new AttributedString(Ansi.ansi().render("@|cyan fd-shell$ |@").toString());
    return builder.toAttributedString();
  }


}
