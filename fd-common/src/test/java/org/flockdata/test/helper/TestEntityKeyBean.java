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
        EntityKeyBean ekb = new EntityKeyBean("docTypeName", "fortress", "code");
        String json = JsonUtils.toJson(ekb);
        EntityKeyBean otherBean = JsonUtils.toObject(json.getBytes(), EntityKeyBean.class);
        TestCase.assertEquals(ekb.getCode(), otherBean.getCode());
        TestCase.assertEquals(ekb.getFortressName(), otherBean.getFortressName());
        TestCase.assertEquals(ekb.getDocumentType(), otherBean.getDocumentType());
    }
}
