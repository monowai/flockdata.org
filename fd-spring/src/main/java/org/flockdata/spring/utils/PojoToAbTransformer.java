/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.spring.utils;

import org.flockdata.helper.FlockException;
import org.flockdata.spring.annotations.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
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
     * "data": "{\"999\": \"99\", \"thingy\": {\"status\": \"tweedle\"}}"
     * }
     * <p/>
     * }
     */

    public static EntityInputBean transformToAbFormat(Object pojo) throws IllegalAccessException, IOException, FlockException {

        //ToDo: LogResultBean is only called when the @key is null, otherwise it's a log
        //ToDo:  caller does not determine this by fd-spring does.

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
                    entityInputBean.setDocumentType(new DocumentTypeInputBean(aClass.getSimpleName().toLowerCase()));
                } else {
                    entityInputBean.setDocumentType(new DocumentTypeInputBean(auditableAnnotation.documentType()));
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
                                entityInputBean.setKey(field.get(pojo).toString());
                            }

                            if (fieldAnnotation instanceof DatagioCallerRef) {
                                auditWhat = false;
                                entityInputBean.setCode(field.get(pojo).toString());
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
        contentInputBean.setData(mapWhat);
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
     * "data": "{\"name\": \"99\", \"thing\": {\"status\": \"android\"}}"
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
                                contentInputBean.setKey(field.get(pojo).toString());
                            }
                            if (fieldAnnotation instanceof DatagioCallerRef) {
                                contentInputBean.setCode(field.get(pojo).toString());
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
        contentInputBean.setData(mapWhat);
        return contentInputBean;
    }
}
