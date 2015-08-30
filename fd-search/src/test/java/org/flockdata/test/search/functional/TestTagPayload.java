package org.flockdata.test.search.functional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by mike on 19/08/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestTagPayload extends ESBase {

    @Test
    public void nestedTag_SearchDoc () throws Exception {
        System.out.print("implement me");
    }
}
