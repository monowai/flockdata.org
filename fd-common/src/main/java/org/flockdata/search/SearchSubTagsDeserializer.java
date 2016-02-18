package org.flockdata.search;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.SearchTag;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mike on 22/08/15.
 */
public class SearchSubTagsDeserializer extends JsonDeserializer <Map<String, Collection<SearchTag>>>  {
    @Override
    public Map<String, Collection<SearchTag>> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Map<String, Collection<SearchTag>> results = new HashMap<>();
        JsonNode node = jp.getCodec().readTree(jp);

        Iterator<Map.Entry<String,JsonNode>> columns = node.fields();

        while (columns.hasNext()){
            Map.Entry<String, JsonNode> row = columns.next();
            String key = row.getKey();
            Collection<SearchTag> tags =  JsonUtils.toCollection(row.getValue().toString(), SearchTag.class);
            //mapper.readValue(next.getValue().toString(), SearchTag.class);
            results.put(key, tags);
        }
        return results;

    }
}
