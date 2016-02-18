package org.flockdata.store;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;

/**
 * Used to talk with the fd-store about entity content
 * Created by mike on 17/02/16.
 */
public class LogRequest {

    private Long logId;
    private Store store;
    private Entity entity;
    private String contentType;
    private String checkSum;

    public LogRequest(){}

    public LogRequest(Entity entity, Log log ){
        this();
        this.logId = log.getId();
        this.store = Store.valueOf(log.getStorage());
        this.entity = entity;
        this.contentType = log.getContentType();
        this.checkSum = log.getChecksum();
    }

    public Long getLogId() {
        return logId;
    }

    public Store getStore() {
        return store;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "LogRequest{" +
                "store=" + store +
                ", logId=" + logId +
                ", entity=" + entity.getMetaKey() +
                '}';
    }

    public String getContentType() {
        return contentType;
    }

    public String getCheckSum() {
        return checkSum;
    }

}
