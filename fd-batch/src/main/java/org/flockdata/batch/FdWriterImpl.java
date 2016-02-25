package org.flockdata.batch;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.batch.resources.FdWriter;
import org.flockdata.client.Importer;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Initialises a FlockData loader to do the writes to the service
 *
 * Initialise from a client.config that exists somewhere on your path
 *
 * Created by nabil on 19/01/2016.
 */
@Component
@Profile("!dev")
public class FdWriterImpl implements FdWriter {

    private FdLoader fdLoader;

    @Autowired
    BatchConfig batchConfig;

    @PostConstruct
    private void init() throws ArgumentParserException, IOException, ClassNotFoundException {
        // These args should come from a Configuration, not processed like this
        // ToDo: Inject ClientConfiguration in to the FDWriter
        ClientConfiguration configuration = batchConfig.getClientConfig();
        // ToDo: Inject FdLoader
        org.flockdata.transform.FdWriter restClient = getRestClient(configuration);
        fdLoader = new FdLoader(restClient, configuration);

    }

    public void write(EntityInputBean item) throws FlockException {
        fdLoader.batchEntity(item); // Passing true ignores the -b batch size param`eter and immediately writes
    }

    public ContentProfile getContentProfile(String name) throws IOException, ClassNotFoundException {
        ContentProfile result =batchConfig.getStepConfig(name).getContentProfile();
        if ( result == null )
            throw new ClassNotFoundException("Unable to resolve the content profile mapping for "+name.toLowerCase());
        return result;
    }

    private org.flockdata.transform.FdWriter getRestClient(ClientConfiguration configuration) {
        return Importer.getRestClient(configuration);
    }

    public FdLoader getFdLoader () {
        return fdLoader;
    }

    public void flush() throws FlockException {
        fdLoader.flush();
    }
}
