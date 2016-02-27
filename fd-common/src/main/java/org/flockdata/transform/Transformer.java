/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.csv.EntityMapper;
import org.flockdata.transform.json.JsonEntityMapper;
import org.flockdata.transform.tags.TagMapper;
import org.flockdata.transform.xml.XmlMappable;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 18/12/15.
 */
public class Transformer {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Transformer.class);

    public static Mappable getMappable(ContentProfile profile) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Mappable mappable = null;

        if (!(profile.getHandler() == null || profile.getHandler().equals("")))
            mappable = (Mappable) Class.forName(profile.getHandler()).newInstance();
        else if (profile.getDocumentType() != null ) {
            mappable = EntityMapper.newInstance(profile);
        } else {
            mappable = TagMapper.newInstance(profile);
        }


        return mappable;

    }

    public static EntityInputBean transformToEntity(Map<String, Object> row, ContentProfile importProfile) throws FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Mappable mappable = getMappable(importProfile);
        Map<String, Object> jsonData = mappable.setData(row, importProfile);
        if ( jsonData == null )
            return null;// No entity did not get created

        EntityInputBean entityInputBean = (EntityInputBean) mappable;

        if (importProfile.isEntityOnly() || jsonData.isEmpty()) {
            entityInputBean.setEntityOnly(true);
            // It's all Meta baby - no log information
        } else {
            String updatingUser = entityInputBean.getUpdateUser();
            if (updatingUser == null)
                updatingUser = (entityInputBean.getFortressUser() == null ? importProfile.getFortressUser() : entityInputBean.getFortressUser());

            ContentInputBean contentInputBean = new ContentInputBean(updatingUser, (entityInputBean.getWhen() != null ? new DateTime(entityInputBean.getWhen()) : null), jsonData);
            contentInputBean.setEvent(importProfile.getEvent());
            entityInputBean.setContent(contentInputBean);
        }
        return entityInputBean;

    }

    public static TagInputBean transformToTag(Map<String, Object> row, ContentProfile importProfile) throws FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Mappable mappable = getMappable(importProfile);
        mappable.setData(row, importProfile);
        return (TagInputBean) mappable;

    }

    public static EntityInputBean transformToEntity(JsonNode node, ContentProfile importProfile) throws FlockException {
        JsonEntityMapper entityInputBean = new JsonEntityMapper();
        entityInputBean.setData(node, importProfile);
        if (entityInputBean.getFortressName() == null)
            entityInputBean.setFortressName(importProfile.getFortressName());
        ContentInputBean contentInputBean = new ContentInputBean();
        if (contentInputBean.getFortressUser() == null)
            contentInputBean.setFortressUser(importProfile.getFortressUser());
        entityInputBean.setContent(contentInputBean);
        contentInputBean.setData(FdJsonObjectMapper.getObjectMapper().convertValue(node, Map.class));

        return entityInputBean;

    }

    public static EntityInputBean transformToEntity(XmlMappable mappable, XMLStreamReader xsr, ContentProfile importProfile) throws FlockException, JAXBException, JsonProcessingException, IllegalAccessException, InstantiationException, ClassNotFoundException {

        XmlMappable row = mappable.newInstance(importProfile);
        ContentInputBean contentInputBean = row.setXMLData(xsr, importProfile);
        EntityInputBean entityInputBean = (EntityInputBean) row;

        if (entityInputBean.getFortressName() == null)
            entityInputBean.setFortressName(importProfile.getFortressName());

        if (entityInputBean.getFortressUser() == null)
            entityInputBean.setFortressUser(importProfile.getFortressUser());


        if (contentInputBean != null) {
            if (contentInputBean.getFortressUser() == null)
                contentInputBean.setFortressUser(importProfile.getFortressUser());
            entityInputBean.setContent(contentInputBean);
        }
        return entityInputBean;
    }

    public static Map<String, Object> convertToMap(String[] headerRow, String[] line, ContentProfile profileConfig) {
        int col = 0;
        Map<String, Object> row = new HashMap<>();
        try {
            for (String column : headerRow) {
                column = column.trim();
                // Find first by the name (if we're using a raw header
                ColumnDefinition colDef = profileConfig.getColumnDef(column);
                if (colDef == null)
                    // Might be indexed by column number if there was no csv
                    colDef = profileConfig.getColumnDef(Integer.toString(col));

                Object value = line[col];
                value = TransformationHelper.transformValue(value, column, colDef);
                boolean addValue = true;
                if (profileConfig.isEmptyIgnored()) {
                    if (value == null || value.toString().trim().equals(""))
                        addValue = false;
                }
                if (addValue) {
                    row.put(column, (value instanceof String ? ((String) value).trim() : value));
                }

                col++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Column does not exist for this row

        }

        return row;
    }
}
