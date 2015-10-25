package org.flockdata.test.helper;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;

/**
 * Created by mike on 23/10/15.
 */
public class TestEntityKeyBean {

    @Test
    public void nullCompanyIssues() throws Exception{
        EntityKeyBean ekb = new EntityKeyBean("callerRef");
        String json = JsonUtils.getJSON(ekb);
        EntityKeyBean otherBean = JsonUtils.getBytesAsObject(json.getBytes(), EntityKeyBean.class);
        TestCase.assertEquals(ekb.getCode(), otherBean.getCode());
    }
}
