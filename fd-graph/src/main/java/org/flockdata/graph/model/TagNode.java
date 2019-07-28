package org.flockdata.graph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Alias;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagHelper;

/**
 * @author mikeh
 * @since 2018-11-19
 */
@Data
@Builder
public class TagNode implements Tag {
  Map<String, Collection<Tag>> subTags = new HashMap<>();
  private Long id;
  private String code;
  private String name;
  private Map<String, Object> properties;
  private String index;
  private String key;
  private ArrayList<String> labels = new ArrayList<>();
  //    @RelatedTo(elementClass = AliasNode.class, type = "HAS_ALIAS")
  private Set<Alias> aliases = new HashSet<>();
  private Boolean isNew = true;

  @Override
  @JsonIgnore
  public Boolean isNew() {
    return isNew;
  }

  @Override
  public void addProperty(String key, Object property) {
    properties.put(key, property);
  }

  public void addAlias(Alias newAlias) {
    aliases.add(newAlias);
  }

  @Override
  @JsonIgnore
  public Map<String, Collection<Tag>> getSubTags() {
    return subTags;
  }

  @Override
  @JsonIgnore
  public Collection<Tag> getSubTags(String key) {
    return subTags.get(key);
  }

  @Override
  public boolean hasSubTags() {
    return (subTags != null && !subTags.isEmpty());
  }

  @Override
  public boolean hasProperties() {
    return (getProperties() != null && !getProperties().isEmpty());
  }

  @Override
  public String getLabel() {
    return TagHelper.getLabel(labels);
  }


}
