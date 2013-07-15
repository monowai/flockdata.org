package com.auditbucket.engine.service;

import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.search.AuditChange;

public class AuditLogVo {
	AuditLogInputBean auditLogInputBean;
	AuditChange auditChange;
	public AuditLogInputBean getAuditLogInputBean() {
		return auditLogInputBean;
	}
	public void setAuditLogInputBean(AuditLogInputBean auditLogInputBean) {
		this.auditLogInputBean = auditLogInputBean;
	}
	public AuditChange getAuditChange() {
		return auditChange;
	}
	public void setAuditChange(AuditChange auditChange) {
		this.auditChange = auditChange;
	}
	
}
