package com.auditbucket.spring.bean;

import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.TrackResultBean;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 05/09/13
 * Time: 23:18
 * To change this template use File | Settings | File Templates.
 */
class MetaResultBean {

    private TrackResultBean trackResultBean;
    private LogInputBean logInputBean;

    public MetaResultBean(TrackResultBean trackResultBean) {
        this.trackResultBean = trackResultBean;
    }

    public MetaResultBean(LogInputBean logInputBean) {
        this.logInputBean = logInputBean;
    }

    public String getMetaKey() {
        if (logInputBean != null) {
            return logInputBean.getMetaKey();
        } else {
            return trackResultBean.getMetaKey();
        }
    }

    public Object getResult() {
        if (logInputBean != null) {
            return logInputBean;
        } else {
            return trackResultBean;
        }
    }
}
