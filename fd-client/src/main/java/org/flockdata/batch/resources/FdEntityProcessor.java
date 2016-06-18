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

package org.flockdata.batch.resources;

import org.flockdata.batch.BatchConfig;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.Transformer;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @author nabil
 */
@Component
@Profile("fd-batch")
public class FdEntityProcessor implements ItemProcessor<Map<String, Object>, EntityInputBean> {

    @Autowired
    private BatchConfig batchConfig;

    private String stepName;

    @Override
    public EntityInputBean process(Map<String, Object> item) throws Exception {
        // This should be initialised just the once
        ContentModel contentModel = getContentModel(stepName);
        return Transformer.transformToEntity(item, contentModel);

    }

    private ContentModel getContentModel(String name) throws IOException, ClassNotFoundException {
        ContentModel result =batchConfig.getStepConfig(name).getContentModel();
        if ( result == null )
            throw new ClassNotFoundException("Unable to resolve the content profile mapping for "+name.toLowerCase());
        return result;
    }


    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepName = stepExecution.getStepName();
    }
}
