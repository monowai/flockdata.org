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

package org.flockdata.batch;

import org.flockdata.profile.model.ContentModel;

/**
 * Created by mike on 25/02/16.
 */
public class StepConfig {
    String query;
    String step;
    String model;
    String modelKey;

    private ContentModel contentModel;

    StepConfig (){}

    StepConfig (String step, String modelKey, String query){
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
     *
     * @return endpoint to look for serverside content model
     */
    public String getModelKey() {
        return modelKey;
    }

    public StepConfig setContentModel(ContentModel contentModel){
        this.contentModel = contentModel;
        return this;
    }

    public ContentModel getContentModel() {
        return contentModel;
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
        if (this == o) return true;
        if (!(o instanceof StepConfig)) return false;

        StepConfig that = (StepConfig) o;

        if (step != null ? !step.equals(that.step) : that.step != null) return false;
        return model != null ? model.equals(that.model) : that.model == null;

    }

    @Override
    public int hashCode() {
        int result = step != null ? step.hashCode() : 0;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        return result;
    }
}
