package org.flockdata.transform;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FlockDataJsonFactory;

import java.io.IOException;

/**
 * Created by mike on 29/07/15.
 */
public class GeoDeserializer extends JsonDeserializer<GeoPayload> {

    @Override
    public GeoPayload deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        ObjectMapper om = FlockDataJsonFactory.getObjectMapper();
        return om.readValue(node.toString(), GeoPayload.class);
    }
}
