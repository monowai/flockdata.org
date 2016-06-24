package org.flockdata.test.unit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 17/12/15.
 */
public class TestCalculatedColumns extends AbstractImport {
    @Test
    public void string_headerWithDelimiter() throws Exception {
        // DAT-527

        ContentModel params = ContentModelDeserializer.getContentModel("/model/calculatedcolumns.json");

        long rows = fileProcessor.processFile(new ExtractProfileHandler(params), "/data/calculatedcolumns.csv");
        int expectedRows = 1;
        assertEquals(expectedRows, rows);
        List<EntityInputBean> entityInputBeans = fdBatcher.getEntities();

        for (EntityInputBean entityInputBean : entityInputBeans) {
            //BulkHours,ScheduledHours,Hours
            TestCase.assertEquals(1d, entityInputBean.getContent().getData().get("BulkHours"));
            TestCase.assertEquals(8.5d, entityInputBean.getContent().getData().get("ScheduledHours"));
            TestCase.assertEquals(9d, entityInputBean.getContent().getData().get("Hours"));
            // VarianceHours is a dynamic column
            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getData().get("VarianceHours"));
            TestCase.assertEquals(.5d, entityInputBean.getContent().getData().get("VarianceHours"));

            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getData().get("WorkHours"));
            TestCase.assertEquals(10d, entityInputBean.getContent().getData().get("WorkHours"));
            TestCase.assertEquals("Value should have come from the calculated column", 10d, entityInputBean.getProperties().get("value"));
        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entityInputBeans);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }

}
