package org.flockdata.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 17/12/15.
 */
public class TestCalculatedColumns extends AbstractImport {
    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        // DAT-527
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/calculatedcolumns.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);


        ImportProfile params = ProfileReader.getImportProfile("/profile/calculatedcolumns.json");

        long rows = fileProcessor.processFile(params, "/data/calculatedcolumns.csv", getFdWriter(), null, configuration);
        int expectedRows = 1;
        assertEquals(expectedRows, rows);
        List<EntityInputBean> entityInputBeans = getFdWriter().getEntities();

        for (EntityInputBean entityInputBean : entityInputBeans) {
            //BulkHours,ScheduledHours,Hours
            TestCase.assertEquals(1d, entityInputBean.getContent().getWhat().get("BulkHours"));
            TestCase.assertEquals(8.5d, entityInputBean.getContent().getWhat().get("ScheduledHours"));
            TestCase.assertEquals(9d, entityInputBean.getContent().getWhat().get("Hours"));
            // VarianceHours is a dynamic column
            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getWhat().get("VarianceHours"));
            TestCase.assertEquals(.5d, entityInputBean.getContent().getWhat().get("VarianceHours"));

            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getWhat().get("WorkHours"));
            TestCase.assertEquals(10d, entityInputBean.getContent().getWhat().get("WorkHours"));
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
