package org.flockdata.search.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.SearchTag;
import org.flockdata.track.bean.EntityKeyBean;

/**
 * @author mikeh
 * @since 6/05/18
 */
public class EntityChangeMapper {
  /**
   * Converts a user requested searchChange in to a standardised document to index
   *
   * @param searchChange searchChange
   * @return document to index
   */
  public static Map<String, Object> getMapFromChange(EntitySearchChange searchChange) {
    Map<String, Object> indexMe = new HashMap<>();
    indexMe.put(SearchSchema.FORTRESS, searchChange.getFortressName());
    // MKH - let elasticSearch manage this
//        indexMe.put(SearchSchema.DOC_TYPE, searchChange.getType());
    if (searchChange.getKey() != null) //DAT-83 No need to track NULL key
    // This occurs if the search doc is not being tracked in fd-engine's graph
    {
      indexMe.put(SearchSchema.KEY, searchChange.getKey());
    }

    if (searchChange.getData() != null) {
      indexMe.put(SearchSchema.DATA, searchChange.getData());
    }
    if (searchChange.getProps() != null && !searchChange.getProps().isEmpty()) {
      indexMe.put(SearchSchema.PROPS, searchChange.getProps());
    }
    if (searchChange.getWho() != null) {
      indexMe.put(SearchSchema.WHO, searchChange.getWho());
    }
    if (searchChange.getEvent() != null) {
      indexMe.put(SearchSchema.LAST_EVENT, searchChange.getEvent());
    }

    // When the Entity was created in the fortress
    indexMe.put(SearchSchema.CREATED, searchChange.getCreatedDate());
    if (searchChange.getUpdatedDate() != null) {
      indexMe.put(SearchSchema.UPDATED, searchChange.getUpdatedDate());
    }

    if (searchChange.hasAttachment()) { // DAT-159
      indexMe.put(SearchSchema.ATTACHMENT, searchChange.getAttachment());
      indexMe.put(SearchSchema.FILENAME, searchChange.getFileName());
      indexMe.put(SearchSchema.CONTENT_TYPE, searchChange.getContentType());
    }

    // Time that this change was indexed by fd-engine
    indexMe.put(SearchSchema.TIMESTAMP, new Date(searchChange.getSysWhen()));

    if (searchChange.getCode() != null) {
      indexMe.put(SearchSchema.CODE, searchChange.getCode());
    }

    String name = searchChange.getName();
    String description = searchChange.getDescription();

    if (name != null) {
      // We prefer storing the description in the search doc
      if (description == null || StringUtils.equals(name, description)) {
        description = name;
      } else {
        indexMe.put(SearchSchema.NAME, searchChange.getName());
      }
    }
    if (description != null) // Search description - generally always returned
    {
      indexMe.put(SearchSchema.DESCRIPTION, searchChange.getDescription());
    }

    if (!searchChange.getTagValues().isEmpty()) {
      setTags("", indexMe, searchChange.getTagValues());
    }

    if (!searchChange.getEntityLinks().isEmpty()) {
      setEntityLinks(indexMe, searchChange.getEntityLinks());
    }

    return indexMe;
  }

  private static void setEntityLinks(Map<String, Object> indexMe, Collection<EntityKeyBean> entityLinks) {

    for (EntityKeyBean linkedEntity : entityLinks) {
      //String prefix;

      Map<String, Map<String, Object>> entity = (Map<String, Map<String, Object>>) indexMe.get("e");
      if (entity == null) {
        entity = new HashMap<>();//
        indexMe.put("e", entity);

      }
      String docType = linkedEntity.getDocumentType().toLowerCase();
      Map<String, Object> docEntry = entity.get(docType);
      if (docEntry == null) {
        docEntry = new HashMap<>();
      }
      entity.put(docType, docEntry);

      Map<String, Object> leaf;
      if (linkedEntity.getRelationshipName() == null || linkedEntity.getRelationshipName().equals("") || linkedEntity.getRelationshipName().equalsIgnoreCase(linkedEntity.getDocumentType())) {
        leaf = docEntry;
        //prefix = "e" +QueryDaoES.ES_FIELD_SEP + linkedEntity.getType().toLowerCase() + QueryDaoES.ES_FIELD_SEP;
      } else {
        leaf = new HashMap<>();
        docEntry.put(linkedEntity.getRelationshipName().toLowerCase(), leaf);

        //prefix = "e" +QueryDaoES.ES_FIELD_SEP+ linkedEntity.getType().toLowerCase() + QueryDaoES.ES_FIELD_SEP + linkedEntity.getRelationship() + QueryDaoES.ES_FIELD_SEP;
      }
      setNonEmptyValue(SearchSchema.CODE, linkedEntity.getCode(), leaf);
      setNonEmptyValue(SearchSchema.INDEX, linkedEntity.getIndex(), leaf);
      setNonEmptyValue(SearchSchema.DESCRIPTION, linkedEntity.getDescription(), leaf);
      setNonEmptyValue("name", linkedEntity.getName(), leaf);
      setTags("", leaf, linkedEntity.getSearchTags());
    }
  }

  private static void setNonEmptyValue(String key, Object value, Map<String, Object> values) {
    if (value != null && !value.toString().equals("")) {
      values.put(key, value);
    }
  }

  private static void setTags(String prefix, Map<String, Object> indexMe, HashMap<String, Map<String, ArrayList<SearchTag>>> tagValues) {

    Collection<String> uniqueTags = new ArrayList<>();
    Collection<String> outputs = new ArrayList<>();
    boolean oneToMany = true;   // Kibana presentation only. ToDo: Set from TagInputBean->SearchTag
    // If false, makes the presentation look better in Kibana.
    // Consider one to one then one to many. One to one displays flat
    // while one to many assumes the tags to be an []. Presentation should strive to be consistent
    // but you can still query consistently either way.

    Map<String, Object> byRelationship = new HashMap<>();
    Map<String, Object> squash = new HashMap<>();
    boolean enableSquash = true; // If tag and rlx names are the same, store just the values
    // means sold.sold.name will appear as sold.name

    for (String relationship : tagValues.keySet()) {
      if (enableSquash && tagValues.get(relationship).containsKey(relationship)) {
        // DAT-328 - the relationship and label have the same name
        ArrayList<SearchTag> values = tagValues.get(relationship).get(relationship);
        if (values.size() == 1 & !oneToMany) {
          // DAT-329
          SearchTag searchTag = values.iterator().next();
          squash.put(relationship, searchTag);
          gatherTag(uniqueTags, searchTag);

        } else {
          ArrayList<SearchTag> searchTags = tagValues.get(relationship).get(relationship);
          squash.put(relationship, searchTags);
          gatherTags(uniqueTags, searchTags);

        }
      } else {
        Map<String, ArrayList<SearchTag>> mapValues = tagValues.get(relationship);
        Map<String, Object> newValues = new HashMap<>();
        for (String label : mapValues.keySet()) {
          if (mapValues.get(label).size() == 1 & !oneToMany) {
            // DAT-329 if only one value, don't store as a collection
            SearchTag searchTag = mapValues.get(label).iterator().next();
            // Store the tag
            newValues.put(label, searchTag);
            gatherTag(uniqueTags, searchTag);
          } else {
            ArrayList<SearchTag> searchTags = mapValues.get(label);
            newValues.put(label, searchTags);
            gatherTags(uniqueTags, searchTags);

          }
        }
        byRelationship.put(relationship, newValues);

      }
    }
    if (!squash.isEmpty()) {
      byRelationship.putAll(squash);
    }
    if (!byRelationship.isEmpty()) {
      indexMe.put(prefix + SearchSchema.TAG, byRelationship);
    }
    //
    if (prefix.equals("") && !uniqueTags.isEmpty()) {
      // ALL_TAGS contains autocomplete searchable tags.
      // ToDo: Prefix == null check stops linked entity tags being written to this list
      indexMe.put(SearchSchema.ALL_TAGS, uniqueTags);
    }


  }

  /**
   * adds a unique tag to the list of tags to store with this document
   * These tags can be search by autocomplete
   *
   * @param tagCodes modified by reference
   * @param fromTags tags to analyze
   */
  private static void gatherTags(Collection<String> tagCodes, Collection<SearchTag> fromTags) {
    for (SearchTag value : fromTags) {
      gatherTag(tagCodes, value);
    }
  }

  private static void gatherTag(Collection<String> tagCodes, SearchTag tag) {
    // ToDo: externalise config
    final int minTagLength = 2;
    if (!tagCodes.contains(tag.getCode())) {
      if (tag.getCode() != null && tag.getCode().length() >= minTagLength) {
        // DAT-446 - Favour the description over a numeric tag code
        // MKH - let fd-engine decide this
        //boolean isAlphaNumeric = true;//!NumberUtils.isNumber(tag.getCode());
        // Always store the code if there is no description or it is Alpha Numeric
        //if ( tag.getName() == null || isAlphaNumeric )
        tagCodes.add(tag.getCode());
      }
      if (tag.getName() != null) {
        String key = tag.getName();
        if (!tagCodes.contains(key)) {
          tagCodes.add(key);
        }
      }
    }
    if (tag.getGeo() != null) {
      Map<String, Object> geoBeans = tag.getGeo();
      for (String key : geoBeans.keySet()) {
//                GeoDataBean geoBean = geoBeans.get(key);
        String code = geoBeans.get(key).toString();
        if (!tagCodes.contains(code)) {
          // ToDo: Figure out autocomplete across ngrams
          // DAT-501 ES 2.0 does not support field names with a .
          if (key.endsWith("Code")) {
            String nameKey = key.substring(0, key.indexOf("Code")) + "Name";
            String name = null;
            if (geoBeans.containsKey(nameKey)) {
              name = geoBeans.get(nameKey).toString();
            }

            tagCodes.add(code + (name != null ? " - " + name : ""));
            if (name != null) {
              tagCodes.add(name + " - " + code);
            }
          }
        }
      }
    }
    if (!tag.getParent().isEmpty()) {
      for (String key : tag.getParent().keySet()) {
        Collection<SearchTag> nestedSearchTags = tag.getParent().get(key);
        gatherTags(tagCodes, nestedSearchTags);
//                LOGGER.info(key);
      }
    }
  }
}
