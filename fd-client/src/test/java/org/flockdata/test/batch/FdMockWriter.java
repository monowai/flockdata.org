package org.flockdata.test.batch;

import org.flockdata.batch.FdWriterImpl;
import org.flockdata.helper.FlockException;
import org.flockdata.test.client.MockFdWriter;
import org.flockdata.track.bean.EntityInputBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by nabil on 19/01/2016.
 */
@Component
@Profile("dev")
public class FdMockWriter extends FdWriterImpl {

    private MockFdWriter fdWriter = null;

    MockFdWriter getFdWriter() {
        return fdWriter;
    }

    @Override
    public void write(EntityInputBean item) throws FlockException {
        getFdLoader().batchEntity(item);// Not writing to FD. Transformed entity will be stored in the fdWriter
    }

    @Override
    public void flush() throws FlockException {
        // Normally would clear the internal payload
        // noop
    }

}
