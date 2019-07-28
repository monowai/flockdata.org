/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.batch;

import org.flockdata.data.ContentModel;

/**
 * @author mholdsworth
 * @since 25/02/2016
 */
public class StepConfig {
  private String query;
  private String step;
  private String model;
  private String modelKey;

  private ContentModel contentModel;

  StepConfig() {
  }

  StepConfig(String step, String modelKey, String query) {
    this();
    this.query = query;
    this.step = step;
    this.model = modelKey;
  }

  public String getQuery() {
    return query;
  }

  public String getStep() {
    return step;
  }

  public String getModel() {
    return model;
  }

  /**
   * @return endpoint to look for serverside content model
   */
  String getModelKey() {
    return modelKey;
  }

  public ContentModel getContentModel() {
    return contentModel;
  }

  public StepConfig setContentModel(ContentModel contentModel) {
    this.contentModel = contentModel;
    return this;
  }

  @Override
  public String toString() {
    return "StepConfig{" +
        "step='" + step + '\'' +
        ", model='" + model + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StepConfig)) {
      return false;
    }

    StepConfig that = (StepConfig) o;

    if (step != null ? !step.equals(that.step) : that.step != null) {
      return false;
    }
    return model != null ? model.equals(that.model) : that.model == null;

  }

  @Override
  public int hashCode() {
    int result = step != null ? step.hashCode() : 0;
    result = 31 * result + (model != null ? model.hashCode() : 0);
    return result;
  }
}
