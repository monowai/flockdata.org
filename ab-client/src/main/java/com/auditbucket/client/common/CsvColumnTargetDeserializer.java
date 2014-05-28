package com.auditbucket.client.common;

import com.auditbucket.client.csv.CsvTag;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 4:25 PM
 */
public class CsvColumnTargetDeserializer extends JsonDeserializer<ArrayList<CsvTag>> {
    @Override
    public ArrayList<CsvTag> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ArrayList<CsvTag> values = new ArrayList<>();
        JsonNode node = jp.getCodec().readTree(jp);
        ObjectMapper om = new ObjectMapper();
        for (JsonNode jsonNode : node) {
            values.add(om.readValue(jsonNode.toString(), CsvTag.class));

        }
        return values;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
