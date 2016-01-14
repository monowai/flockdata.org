package org.flockdata.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.profile.ImportProfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles ProfileConfiguration handling
 *
 * Created by mike on 13/01/16.
 */
public class ProfileReader {
    /**
     * Reads an ImportProfile JSON file and returns the Pojo
     *
     * @param profile Fully file name
     * @return initialized ImportProfile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ImportProfile getImportProfile(String profile) throws IOException, ClassNotFoundException {
        ImportProfile importProfile;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else {
                throw new IllegalArgumentException("Unable to locate the profile " + profile);
            }
        }
        return importProfile;
    }
}
