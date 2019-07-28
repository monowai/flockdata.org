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

package org.flockdata.model;

import java.util.Collection;
import org.flockdata.transform.ColumnDefinition;

/**
 * Encapsulates the result of a single validation run
 *
 * @author mholdsworth
 * @since 14/04/2016
 */
public class ColumnValidationResult {
  private String sourceColumn;
  private ColumnDefinition column;
  private Collection<String> messages;
  private String expression;

  public ColumnValidationResult() {

  }

  public ColumnValidationResult(String sourceColumn, ColumnDefinition column, Collection<String> messages) {
    this();
    this.sourceColumn = sourceColumn;
    this.column = column;
    this.messages = messages;
  }

  public ColumnDefinition getColumn() {
    return column;
  }


  public void setMessage(Collection<String> messages) {
    this.messages = messages;
  }

  public String getSourceColumn() {
    return sourceColumn;
  }

  public Collection<String> getMessages() {
    return messages;
  }

  @Override
  public String toString() {
    return "ContentValidationResult{" +
        "sourceColumn=" + sourceColumn +
        '}';
  }

  public String getExpression() {
    return expression;
  }

  public ColumnValidationResult setExpression(String expression) {
    this.expression = expression;
    return this;
  }
}
