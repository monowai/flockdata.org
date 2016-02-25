package org.flockdata.batch.resources;

import org.flockdata.profile.model.ContentProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.Transformer;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @author nabil
 */
public class FlockDataItemProcessor implements ItemProcessor<Map<String, Object>, EntityInputBean> {

    @Autowired
    private FdWriter batchFdLoader;

    private String stepName;

    @Override
    public EntityInputBean process(Map<String, Object> item) throws Exception {
        // This should be initialised just the once
        ContentProfile contentProfile = batchFdLoader.getContentProfile(stepName);
        return Transformer.transformToEntity(item, contentProfile);

    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepName = stepExecution.getStepName();
    }
}
