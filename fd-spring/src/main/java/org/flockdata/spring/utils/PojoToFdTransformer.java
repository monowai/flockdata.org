/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.spring.utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.helper.FlockException;
import org.flockdata.spring.annotations.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

@Component
public class PojoToFdTransformer {

  /**
   * @param pojo data object to transform
   * @return EntityInputBean to track to FlockData
   * @throws IllegalAccessException error
   * @throws FlockException         error
   */

  public static EntityInputBean transformEntity(Object pojo) throws FlockException, IllegalAccessException {

    //ToDo: LogResultBean is only called when the @key is null, otherwise it's a log
    //ToDo:  caller does not determine this by fd-spring does.

    EntityInputBean entityInputBean = new EntityInputBean();
    ContentInputBean contentInputBean = new ContentInputBean("null", new DateTime(), null);
    Map<String, Object> tagValues = new HashMap<>();
    Map<String, Object> mapWhat = new HashMap<>();
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
              if (fieldAnnotation instanceof FdUid) {
                auditWhat = false;
                entityInputBean.setKey(field.get(pojo).toString());
              }

              if (fieldAnnotation instanceof FdCallerRef) {
                auditWhat = false;
                entityInputBean.setCode(field.get(pojo).toString());
              }

              // ToDo: AuditUser


              if (fieldAnnotation instanceof FdTag) {
                auditWhat = false;
                FdTag auditTagAnnotation = (FdTag) fieldAnnotation;
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
              if (fieldAnnotation instanceof FdUid || fieldAnnotation instanceof FdTag || fieldAnnotation instanceof NoTrack) {
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
    contentInputBean.setData(mapWhat);
    entityInputBean.setContent(contentInputBean);
    //ToDo: Figure out tag structure
    //metaInputBean.setTagValues(tagValues);
    return entityInputBean;
  }

  /**
   * Logs a data change event in flockdata for the entity
   *
   * @param pojo data to change
   * @return Content data
   * @throws IllegalAccessException error
   * @throws IOException            error
   * @throws FlockException         error
   */
  public static ContentInputBean transformToFdContent(Object pojo) throws IllegalAccessException, IOException, FlockException {
    ContentInputBean contentInputBean = new ContentInputBean("mike", new DateTime(), null);
    Map<String, Object> mapWhat = new HashMap<>();

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
              if (fieldAnnotation instanceof FdUid) {
                contentInputBean.setKey(field.get(pojo).toString());
              }
              if (fieldAnnotation instanceof FdCallerRef) {
                contentInputBean.setCode(field.get(pojo).toString());
              }

              if (fieldAnnotation instanceof NoTrack) {
                auditWhat = false;
              }
            } else {
              if (fieldAnnotation instanceof FdUid || fieldAnnotation instanceof FdCallerRef || fieldAnnotation instanceof FdTag || fieldAnnotation instanceof NoTrack) {
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
