package com.auditbucket.spring.utils;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.spring.annotations.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PojoToAbTransformer {

    /**
     * {
     * "fortress":@auditFortress,
     * "fortressUser": @auditUser,
     * "documentType":@auditDocument or can be passed in during the event,
     * "when": @auditDate,
     *
     * @param pojo
     * @return
     * @throws IllegalAccessException
     * @throws IOException
     * @Tags in the format of "Type"/"Value"
     * "tagValues": { "TypeA": "Helos", "TypeB": "Tiger"},
     * See AuditLog below
     * "auditLog": {
     * "when": "2012-11-12",
     * "transactional": false,
     * "what": "{\"999\": \"99\", \"thingy\": {\"status\": \"tweedle\"}}"
     * }
     * <p/>
     * }
     */

    public static AuditHeaderInputBean transformToAbFormat(Object pojo) throws IllegalAccessException, IOException {

        //ToDo: AuditHeader is only called when the @AuditKey is null, otherwise it's a log
        //ToDo:  caller does not determine this by ab-spring does.

        AuditHeaderInputBean auditHeaderInputBean = new AuditHeaderInputBean();
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean("null", new DateTime(), null);
        Map<String, String> tagValues = new HashMap<String, String>();
        Map<String, Object> mapWhat = new HashMap<String, Object>();
        Class aClass = pojo.getClass();
        Annotation[] annotations = aClass.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Auditable) {
                // Class Is annotated for being send to AB
                Auditable auditableAnnotation = (Auditable) annotation;
                if (auditableAnnotation.documentType().equals("")) {
                    auditHeaderInputBean.setDocumentType(aClass.getSimpleName().toLowerCase());
                } else {
                    auditHeaderInputBean.setDocumentType(auditableAnnotation.documentType());
                }
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
                    boolean auditWhat = true;
                    // If NoAuditAnnotation
                    for (Annotation fieldAnnotation : fieldAnnotations) {
                        // The case when the value of field are not NULL
                        // ToDo: When can we get the Fortress or DocumentType?
                        // ToDo: should be an annotation around the service.method,
                        // ToDo: i.e. @audit (docType="Booking", fortress="Fortress")
                        if (field.get(pojo) != null) {
                            if (fieldAnnotation instanceof AuditKey) {
                                auditWhat = false;
                                auditHeaderInputBean.setAuditKey(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof AuditClientRef) {
                                auditWhat = false;
                                auditHeaderInputBean.setCallerRef(field.get(pojo).toString());
                            }

                            // ToDo: AuditUser


                            if (fieldAnnotation instanceof AuditTag) {
                                auditWhat = false;
                                AuditTag auditTagAnnotation = (AuditTag) fieldAnnotation;
                                if (auditTagAnnotation.name().equals("")) {
                                    tagValues.put(field.getName(), field.get(pojo).toString());
                                } else {
                                    tagValues.put(auditTagAnnotation.name(), field.get(pojo).toString());
                                }
                            }
                            if (fieldAnnotation instanceof NoAudit) {
                                auditWhat = false;
                            }

                        } else {
                            // The case when the value of field are NULL
                            // because we can have TIME=t0 ==> status=STARTED || TIME=t1 ==> status=NULL
                            if (fieldAnnotation instanceof AuditKey || fieldAnnotation instanceof AuditTag || fieldAnnotation instanceof NoAudit) {
                                auditWhat = false;
                            }
                        }
                    }
                    if (auditWhat) {
                        // ToDo: This needs to assume nested objects and recursively look through them as well
                        // ToDo: customer JSON transformer to serialize the entire object to JSON Node
                        // and ignore the fields that are NoAudit, AuditKey.
                        mapWhat.put(field.getName(), field.get(pojo));
                    }
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        String what = mapper.writeValueAsString(mapWhat);
        auditLogInputBean.setWhat(what);
        auditHeaderInputBean.setAuditLog(auditLogInputBean);
        auditHeaderInputBean.setTagValues(tagValues);
        return auditHeaderInputBean;
    }

    /**
     * Maps to the AuditLog event.
     * {
     * "auditKey": @auditKey,
     * "when": @auditDate,
     * "fortressUser": @auditUser,
     * "comment": @canBePassedInDuringTheEvent or @auditComment,
     * "transactional": @canBePassedInDuringTheEvent ,
     *
     * @param pojo
     * @return
     * @throws IllegalAccessException
     * @throws IOException
     * @What is EVERY other attribute and object that is not @audit* annotated
     * "what": "{\"name\": \"99\", \"thing\": {\"status\": \"android\"}}"
     * }
     */
    public static AuditLogInputBean transformToAbLogFormat(Object pojo) throws IllegalAccessException, IOException {
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean("null", new DateTime(), null);
        Map<String, Object> mapWhat = new HashMap<String, Object>();

        Class aClass = pojo.getClass();
        Annotation[] annotations = aClass.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Auditable) {
                // The case when the value of field are not NULL
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    boolean auditWhat = true;
                    Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
                    for (Annotation fieldAnnotation : fieldAnnotations) {
                        // The case when the value of field are not NULL
                        if (field.get(pojo) != null) {
                            if (fieldAnnotation instanceof AuditKey) {
                                auditLogInputBean.setAuditKey(field.get(pojo).toString());
                            }
                            if (fieldAnnotation instanceof AuditClientRef) {
                                auditLogInputBean.setCallerRef(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof NoAudit) {
                                auditWhat = false;
                            }
                        } else {
                            if (fieldAnnotation instanceof AuditKey || fieldAnnotation instanceof AuditClientRef || fieldAnnotation instanceof AuditTag || fieldAnnotation instanceof NoAudit) {
                                auditWhat = false;
                            }
                        }
                    }
                    if (auditWhat) {
                        mapWhat.put(field.getName(), field.get(pojo));
                    }
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        String what = mapper.writeValueAsString(mapWhat);
        auditLogInputBean.setWhat(what);
        return auditLogInputBean;
    }
}
