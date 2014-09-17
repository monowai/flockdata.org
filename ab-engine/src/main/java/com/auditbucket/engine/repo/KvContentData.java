package com.auditbucket.engine.repo;

import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.model.KvContent;

import java.util.Map;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 1:03 PM
 */
public class KvContentData implements KvContent{
    private String attachment;
    Map<String,Object> what;

    private KvContentData(){super();}

    public KvContentData(ContentInputBean content){
        this();
        this.attachment = content.getAttachment();
        this.what = content.getWhat();
    }

    public KvContentData(Map<String, Object> result) {
        this();
        this.what = result;
    }

    public String getAttachment() {
        return attachment;
    }

    public Map<String, Object> getWhat() {
        return what;
    }

}
