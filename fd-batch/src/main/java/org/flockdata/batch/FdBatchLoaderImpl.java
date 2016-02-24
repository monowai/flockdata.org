package org.flockdata.batch;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.batch.resources.FdBatchLoader;
import org.flockdata.client.Importer;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdLoader;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.ProfileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nabil on 19/01/2016.
 */
@Component
@Profile("!dev")
public class FdBatchLoaderImpl implements FdBatchLoader {

    private FdLoader fdLoader;
    private Map<String,ContentProfile> contentProfiles = new HashMap<>();

    @Autowired
    BatchConfig batchConfig;

    @PostConstruct
    private void init() throws ArgumentParserException, IOException, ClassNotFoundException {
        // These args should come from a Configuration, not processed like this
        // ToDo: Inject ClientConfiguration in to the FDWriter
        ClientConfiguration configuration = batchConfig.getClientConfig();
        // ToDo: Inject FdLoader
        FdWriter restClient = getRestClient(configuration);
        fdLoader = new FdLoader(restClient, configuration);

        if ( batchConfig.getContentProfileName() !=null && !batchConfig.getContentProfileName().equals("") ){
            ContentProfileImpl contentProfile = ProfileReader.getImportProfile(batchConfig.getContentProfileName());
            String key = contentProfile.getFortressName() +"."+ contentProfile.getDocumentType().getName();
            contentProfiles.put(key.toLowerCase(), contentProfile);
        }

    }

    public void write(EntityInputBean item) throws FlockException {
        fdLoader.batchEntity(item); // Passing true ignores the -b batch size param`eter and immediately writes
    }

    public ContentProfile getContentProfile(String name) throws IOException, ClassNotFoundException {
        ContentProfile result =contentProfiles.get(name.toLowerCase());
        if ( result == null )
            throw new ClassNotFoundException("Unable to resolve the content profile mapping for "+name.toLowerCase());
        return result;
    }

    private FdWriter getRestClient(ClientConfiguration configuration) {
        return Importer.getRestClient(configuration);
    }

    public FdLoader getFdLoader () {
        return fdLoader;
    }

    public void flush() throws FlockException {
        fdLoader.flush();
    }
}
