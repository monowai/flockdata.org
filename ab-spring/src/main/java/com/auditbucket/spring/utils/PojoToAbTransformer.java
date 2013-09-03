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

    public static AuditHeaderInputBean transformToAbFormat(Object pojo) throws IllegalAccessException, IOException {
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
                    boolean noAuditAnnotation = true;
                    // If NoAuditAnnotation
                    for (Annotation fieldAnnotation : fieldAnnotations) {
                        // The case when the value of field are not NULL
                        if (field.get(pojo) != null) {
                            if (fieldAnnotation instanceof AuditKey) {
                                noAuditAnnotation = false;
                                auditHeaderInputBean.setAuditKey(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof AuditClientRef) {
                                noAuditAnnotation = false;
                                auditHeaderInputBean.setCallerRef(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof AuditTag) {
                                noAuditAnnotation = false;
                                AuditTag auditTagAnnotation = (AuditTag) fieldAnnotation;
                                if (auditTagAnnotation.name().equals("")) {
                                    tagValues.put(field.getName(), field.get(pojo).toString());
                                } else {
                                    tagValues.put(auditTagAnnotation.name(), field.get(pojo).toString());
                                }
                            }
                            if (fieldAnnotation instanceof NoAudit) {
                                noAuditAnnotation = false;
                            }

                        } else {
                            // The case when the value of field are NULL
                            // ONly What attribute is calculated
                            // because we can have TIME=t0 ==> status=STARTED || TIME=t1 ==> status=NULL
                            if (fieldAnnotation instanceof AuditKey || fieldAnnotation instanceof AuditClientRef || fieldAnnotation instanceof AuditTag || fieldAnnotation instanceof NoAudit) {
                                noAuditAnnotation = false;
                            }
                        }
                    }
                    if (noAuditAnnotation) {
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
