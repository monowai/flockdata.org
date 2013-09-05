package com.auditbucket.spring.bean;

import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 05/09/13
 * Time: 23:18
 * To change this template use File | Settings | File Templates.
 */
public class AuditBucketResultBean {

    private AuditResultBean auditResultBean;
    private AuditLogInputBean auditLogInputBean;

    public AuditBucketResultBean(AuditResultBean auditResultBean) {
        this.auditResultBean = auditResultBean;
    }

    public AuditBucketResultBean(AuditLogInputBean auditLogInputBean) {
        this.auditLogInputBean = auditLogInputBean;
    }

    public String getAuditKey() {
        if (auditLogInputBean != null) {
            return auditLogInputBean.getAuditKey();
        } else {
            return auditResultBean.getAuditKey();
        }
    }

    public Object getResult() {
        if (auditLogInputBean != null) {
            return auditLogInputBean;
        } else {
            return auditResultBean;
        }
    }
}
