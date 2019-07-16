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

package org.flockdata.batch.resources;

import java.io.IOException;
import java.util.Map;
import org.flockdata.batch.BatchConfig;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.Transformer;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author nabil
 * @tag Batch, Tag
 */
@Component
@Profile( {"fd-batch", "fd-batch-dev"})
@Service
public class FdTagProcessor implements ItemProcessor<Map<String, Object>, TagInputBean> {

    private final BatchConfig batchConfig;

    private String stepName;

    @Autowired
    public FdTagProcessor(BatchConfig batchConfig) {
        this.batchConfig = batchConfig;
    }

    @Override
    public TagInputBean process(Map<String, Object> item) throws Exception {
        // This should be initialised just the once
        ContentModel contentModel = getContentModel(stepName);
        return Transformer.toTags(item, contentModel).iterator().next();

    }

    private ContentModel getContentModel(String name) throws IOException, ClassNotFoundException {
        ContentModel result = batchConfig.getStepConfig(name).getContentModel();
        if (result == null) {
            throw new ClassNotFoundException(String.format("Unable to resolve the content model [%s] for step [%s]", name, batchConfig.getStepConfig(name).getModel()));
        }
        return result;
    }


    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepName = stepExecution.getStepName();
    }
}
