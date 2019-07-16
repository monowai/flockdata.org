package org.flockdata.test.neo.integ;

import static org.assertj.core.api.Assertions.assertThat;

import org.flockdata.data.Entity;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.dao.EntityRepo;
import org.flockdata.graph.model.EntityNode;
import org.flockdata.integration.Base64;
import org.flockdata.integration.KeyGenService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mikeh
 * @since 10/06/18
 */
@ActiveProfiles( {"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    DriverManager.class,
    KeyGenService.class,
    Base64.class,
    EntityRepo.class}
)

public class TestEntity extends BaseNeo {

    @Autowired
    EntityRepo entityRepo;
    @Autowired
    KeyGenService keyGenService;

    @Test
    public void testEntity() {
        Entity myEntity = EntityNode.builder()
            .name("myName")
            .code("myCode")
            .key(keyGenService.getUniqueKey())
            .type("Person")
            .build();
        Entity created = entityRepo.create(myEntity);
        assertThat(created).isNotNull().hasFieldOrProperty("id");
        Entity found = entityRepo.findByKey(myEntity);
        assertThat(found).isNotNull().hasFieldOrProperty("id");
    }
}
