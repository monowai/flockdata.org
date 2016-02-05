package org.flockdata.profile.model;

/**
 * Created by mike on 28/01/16.
 */
public interface ImportFile {

    ContentProfile.ContentType getContentType();

    char getDelimiter();

    boolean hasHeader();

    String getPreParseRowExp();

    String getQuoteCharacter();


}
