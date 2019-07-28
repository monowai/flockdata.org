package org.flockdata.test.neo.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.flockdata.data.Entity;
import org.flockdata.graph.model.EntityNode;
import org.junit.Test;

/**
 * @author mikeh
 * @since 23/06/18
 */
public class TestEntityPojo {
  @Test
  public void entityNodeDefaults() {
    Entity entity = EntityNode.builder().code("test").build();
    assertThat(entity)
        .hasFieldOrPropertyWithValue("code", "test")
        .hasFieldOrProperty("lastUpdate")
        .hasFieldOrProperty("dateCreated")
        .hasFieldOrProperty("labels");

    assertThat(entity.getDateCreated())
        .isNotEqualTo(0l)
        .isEqualTo(entity.getLastUpdate());
  }
}
