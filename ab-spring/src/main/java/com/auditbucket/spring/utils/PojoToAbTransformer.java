package com.auditbucket.spring.utils;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.spring.annotations.AuditClientRef;
import com.auditbucket.spring.annotations.AuditKey;
import com.auditbucket.spring.annotations.Auditable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class PojoToAbTransformer {

    public static AuditHeaderInputBean transformToAbFormat(Object pojo) throws IllegalAccessException {
        AuditHeaderInputBean auditHeaderInputBean = new AuditHeaderInputBean();
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

                    for (Annotation fieldAnnotation : fieldAnnotations) {
                        if (fieldAnnotation instanceof AuditKey) {
                            auditHeaderInputBean.setAuditKey(field.get(pojo).toString());
                        }
                        if (fieldAnnotation instanceof AuditClientRef) {
                            auditHeaderInputBean.setCallerRef(field.get(pojo).toString());
                        }
                    }
                }
            }
        }
        return auditHeaderInputBean;
    }
}
