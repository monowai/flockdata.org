package org.flockdata.batch.resources;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdLoader;

import java.io.IOException;

/**
 * Created by mike on 22/01/16.
 */
public interface FdWriter {

    void write(EntityInputBean item) throws FlockException;

    ContentProfile getContentProfile(String name) throws IOException, ClassNotFoundException;

    void flush() throws FlockException;

    FdLoader getFdLoader();
}
