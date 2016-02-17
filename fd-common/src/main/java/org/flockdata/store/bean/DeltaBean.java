package org.flockdata.store.bean;

import org.flockdata.model.Log;
import org.flockdata.store.LogRequest;

/**
 * Created by mike on 17/02/16.
 */
public class DeltaBean {
    DeltaBean(){}

    private LogRequest logRequest;
    private Log preparedLog;

    public DeltaBean(LogRequest logRequest, Log preparedLog) {
        this();
        this.logRequest = logRequest;
        this.preparedLog = preparedLog;
    }

    public LogRequest getLogRequest() {
        return logRequest;
    }

    public Log getPreparedLog() {
        return preparedLog;
    }
}
