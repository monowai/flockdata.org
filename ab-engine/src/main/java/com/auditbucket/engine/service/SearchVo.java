package com.auditbucket.engine.service;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.search.AuditChange;

public class SearchVo {
	AuditHeaderInputBean  auditHeaderInputBean;
	AuditChange auditChange;
	public AuditHeaderInputBean getAuditHeaderInputBean() {
		return auditHeaderInputBean;
	}
	public void setAuditHeaderInputBean(AuditHeaderInputBean auditHeaderInputBean) {
		this.auditHeaderInputBean = auditHeaderInputBean;
	}
	public AuditChange getAuditChange() {
		return auditChange;
	}
	public void setAuditChange(AuditChange auditChange) {
		this.auditChange = auditChange;
	}
	
}
