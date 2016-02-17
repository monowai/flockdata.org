package org.flockdata.store.bean;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;

/**
 * Created by mike on 17/02/16.
 */
public class ReadContentBean {
    ReadContentBean(){}

    private Entity entity;
    private Log log;

    public ReadContentBean(Entity entity, Log log) {
        this();
        this.entity = entity;
        this.log =log;
    }
}
