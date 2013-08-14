package com.auditbucket.spring;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 11/08/13
 * Time: 13:56
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbClient {

        public abstract String get() throws Exception ;

        public abstract java.util.Map<String, String> getHealth() throws Exception ;

        public abstract void createHeader() throws Exception ;

        public abstract void createLog() throws Exception ;

        public abstract void getAuditTxLogs(String txRef) throws Exception ;

        public abstract void getAuditTxHeaders(String txRef) throws Exception ;

        public abstract void getAuditTx( String txRef) throws Exception ;

        public abstract void getAudit(String auditKey) throws Exception ;

        public abstract void getByClientRef(String fortress,
                                                           String recordType,
                                                          String callerRef) throws Exception;

        public abstract void getAuditLogs(String auditKey) throws Exception ;

        public abstract void getLastChange(String auditKey) ;

}
