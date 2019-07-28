package org.flockdata.search.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.SearchSchema;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author mikeh
 * @since 11/03/18
 */
@Component
public class EsUtils {

  String[] allTypes = {"*"};

  public String parseException(ExecutionException e) {
    if (e.getCause() != null) {
      return (e.getCause().getCause() != null ? e.getCause().getCause().getMessage() : e.getCause().getMessage());
    }
    return e.getMessage();

  }

  public EntityKeyResults toResults(SearchResponse response) {
    EntityKeyResults results = new EntityKeyResults();
    if (response == null || response.getHits().getTotalHits() == 0) {
      return results;
    }

    for (SearchHit searchHitFields : response.getHits().getHits()) {
      Object o = searchHitFields.getFields().get(SearchSchema.KEY).getValues().iterator().next();
      results.add(o);
    }
    return results;
  }

  public EsSearchRequestResult wrapResponseResult(SearchResponse response) {
    EsSearchRequestResult result = new EsSearchRequestResult(response.toString().getBytes());
    result.setTotalHits(response.getHits().getTotalHits());
    return result;
  }

  public String getHitValue(SearchHitField field) {
    if (field == null || field.getValue() == null) {
      return null;
    }

    return field.getValue().toString();
  }

  public Map<String, HighlightField> convertHighlightToMap(Map<String, HighlightField> highlightFields) {
    Map<String, HighlightField> highlights = new HashMap<>();
    for (String key : highlightFields.keySet()) {
      Text[] esFrag = highlightFields.get(key).getFragments();
      highlights.put(key, new HighlightField(key, esFrag));
    }
    return highlights;
  }


  public Collection<String> parseException(String message) {

    Collection<String> results = new ArrayList<>();
    String[] failures = StringUtils.delimitedListToStringArray(message, "Parse Failure ");
    if (failures.length == 0) {
      results.add(message);
    } else {
      for (String failure : failures) {
        // Exclude duplicates and query source
        if (!results.contains(failure) && !failure.startsWith("[Failed to parse source")) {
          results.add(failure);
        }
      }
    }
    return results;
  }

  public String[] getAllTypes() {

    return allTypes;
  }
}
