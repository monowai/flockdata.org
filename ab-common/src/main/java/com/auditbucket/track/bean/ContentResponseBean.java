package com.auditbucket.track.bean;

import com.auditbucket.track.model.ChangeEvent;
import com.auditbucket.track.model.Log;

import java.util.Date;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 8:58 AM
 */
public class ContentResponseBean {
    private ChangeEvent changeEvent;
    private String fileName;
    private String comment;
    private Date when;
    private String fortress;
    private Log.ContentType contentType;

    ContentResponseBean (){}
    ContentResponseBean (ContentInputBean input) {

        contentType = input.getContentType();
        fileName = input.getFileName();
        fortress = input.getFortress();
        changeEvent = input.getChangeEvent();
        comment = input.getComment();
        when = input.getWhen();
    }
}
