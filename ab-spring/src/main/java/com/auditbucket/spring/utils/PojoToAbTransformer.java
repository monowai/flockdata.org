package com.auditbucket.spring.utils;

import com.auditbucket.helper.FlockException;
import com.auditbucket.spring.annotations.*;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
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
     * @Tags in the format of "Value"/"Type"
     * "tagValues": { "Helos": "TypeA", "Tiger": "AnimalType", "1234", "{"TypeB", "TypeC"}"},
     * See DatagioLog below
     * "auditLog": {
     * "when": "2012-11-12",
     * "transactional": false,
     * "what": "{\"999\": \"99\", \"thingy\": {\"status\": \"tweedle\"}}"
     * }
     * <p/>
     * }
     */

    public static EntityInputBean transformToAbFormat(Object pojo) throws IllegalAccessException, IOException, FlockException {

        //ToDo: LogResultBean is only called when the @DatagioUid is null, otherwise it's a log
        //ToDo:  caller does not determine this by ab-spring does.

        EntityInputBean entityInputBean = new EntityInputBean();
        ContentInputBean contentInputBean = new ContentInputBean("null", new DateTime(), null);
        Map<String, Object> tagValues = new HashMap<String, Object>();
        Map<String, Object> mapWhat = new HashMap<String, Object>();
        Class aClass = pojo.getClass();
        Annotation[] annotations = aClass.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Trackable) {
                // Class Is annotated for being send to AB
                Trackable auditableAnnotation = (Trackable) annotation;
                if (auditableAnnotation.documentType().equals("")) {
                    entityInputBean.setDocumentType(aClass.getSimpleName().toLowerCase());
                } else {
                    entityInputBean.setDocumentType(auditableAnnotation.documentType());
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
                        // ToDo: i.e. @track (docType="Booking", fortress="Fortress")
                        if (field.get(pojo) != null) {
                            if (fieldAnnotation instanceof DatagioUid) {
                                auditWhat = false;
                                entityInputBean.setMetaKey(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof DatagioCallerRef) {
                                auditWhat = false;
                                entityInputBean.setCallerRef(field.get(pojo).toString());
                            }

                            // ToDo: AuditUser


                            if (fieldAnnotation instanceof DatagioTag) {
                                auditWhat = false;
                                DatagioTag auditTagAnnotation = (DatagioTag) fieldAnnotation;
                                // ToDo: Assume all values to be a list. We could be adding a value to an existing key
                                // ToDo: i.e 123ABC/CustRef exists, in a sub object we add 123ABC/Customer
                                // This would create 2 relationships for the tag key 123ABC, not simply replace it.
                                if (auditTagAnnotation.name().equals("")) {
                                    tagValues.put(field.get(pojo).toString(), field.getName());
                                } else {
                                    tagValues.put(field.get(pojo).toString(), auditTagAnnotation.name());
                                }
                            }
                            if (fieldAnnotation instanceof NoTrack) {
                                auditWhat = false;
                            }

                        } else {
                            // The case when the value of field are NULL
                            // because we can have TIME=t0 ==> status=STARTED || TIME=t1 ==> status=NULL
                            if (fieldAnnotation instanceof DatagioUid || fieldAnnotation instanceof DatagioTag || fieldAnnotation instanceof NoTrack) {
                                auditWhat = false;
                            }
                        }
                    }
                    if (auditWhat) {
                        // ToDo: This needs to assume nested objects and recursively look through them as well
                        // ToDo: customer JSON transformer to serialize the entire object to JSON Node
                        // and ignore the fields that are NoTrack, DatagioUid.
                        mapWhat.put(field.getName(), field.get(pojo));
                    }
                }
            }
        }
        //ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        //String what = mapper.writeValueAsString(mapWhat);
        contentInputBean.setWhat(mapWhat);
        entityInputBean.setContent(contentInputBean);
        //ToDo: Figure out tag structure
        //metaInputBean.setTagValues(tagValues);
        return entityInputBean;
    }

    /**
     * Maps to the DatagioLog event.
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
     * @What is EVERY other attribute and object that is not @track* annotated
     * "what": "{\"name\": \"99\", \"thing\": {\"status\": \"android\"}}"
     * }
     */
    public static ContentInputBean transformToAbLogFormat(Object pojo) throws IllegalAccessException, IOException, FlockException {
        ContentInputBean contentInputBean = new ContentInputBean("mike", new DateTime(), null);
        Map<String, Object> mapWhat = new HashMap<String, Object>();

        Class aClass = pojo.getClass();
        Annotation[] annotations = aClass.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Trackable) {
                // The case when the value of field are not NULL
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    boolean auditWhat = true;
                    Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
                    for (Annotation fieldAnnotation : fieldAnnotations) {
                        // The case when the value of field are not NULL
                        if (field.get(pojo) != null) {
                            if (fieldAnnotation instanceof DatagioUid) {
                                contentInputBean.setMetaKey(field.get(pojo).toString());
                            }
                            if (fieldAnnotation instanceof DatagioCallerRef) {
                                contentInputBean.setCallerRef(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof NoTrack) {
                                auditWhat = false;
                            }
                        } else {
                            if (fieldAnnotation instanceof DatagioUid || fieldAnnotation instanceof DatagioCallerRef || fieldAnnotation instanceof DatagioTag || fieldAnnotation instanceof NoTrack) {
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
        //ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        //String what = mapper.writeValueAsString(mapWhat);
        contentInputBean.setWhat(mapWhat);
        return contentInputBean;
    }
}
