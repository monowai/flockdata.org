package com.auditbucket.spring.bean;

import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.TrackResultBean;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 05/09/13
 * Time: 23:18
 * To change this template use File | Settings | File Templates.
 */
class EntityResultBean {

    private TrackResultBean trackResultBean;
    private ContentInputBean contentInputBean;

    public EntityResultBean(TrackResultBean trackResultBean) {
        this.trackResultBean = trackResultBean;
    }

    public EntityResultBean(ContentInputBean contentInputBean) {
        this.contentInputBean = contentInputBean;
    }

    public String getMetaKey() {
        if (contentInputBean != null) {
            return contentInputBean.getMetaKey();
        } else {
            return trackResultBean.getMetaKey();
        }
    }

    public Object getResult() {
        if (contentInputBean != null) {
            return contentInputBean;
        } else {
            return trackResultBean;
        }
    }
}
