package org.flockdata.store.bean;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;

/**
 * Created by mike on 17/02/16.
 */
public class DeltaBean {
    DeltaBean(){}

    private Entity entity;
    private Log log;
    private Log preparedLog;

    public DeltaBean(Entity entity, Log log, Log preparedLog) {
        this();
        this.entity = entity;
        this.log =log;
        this.preparedLog = preparedLog;
    }
}
