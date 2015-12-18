package org.flockdata.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.csv.CsvEntityMapper;
import org.flockdata.transform.json.JsonEntityMapper;
import org.flockdata.transform.tags.TagMapper;
import org.flockdata.transform.xml.XmlMappable;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import java.util.Map;

/**
 * Created by mike on 18/12/15.
 */
public class Transformer {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Transformer.class);

    public static Mappable getMappable(ProfileConfiguration profile) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Mappable mappable = null;

        if (!(profile.getHandler() == null || profile.getHandler().equals("")))
            mappable = (Mappable) Class.forName(profile.getHandler()).newInstance();
        else if (profile.getTagOrEntity() == ProfileConfiguration.DataType.ENTITY) {
            mappable = CsvEntityMapper.newInstance(profile);
        } else if (profile.getTagOrEntity() == ProfileConfiguration.DataType.TAG) {
            mappable = TagMapper.newInstance(profile);
        } else
            logger.error("Unable to determine the implementing handler");


        return mappable;

    }

    public static EntityInputBean transformToEntity(Map<String, Object> row, ProfileConfiguration importProfile) throws FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
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

    public static TagInputBean transformToTag(Map<String, Object> row, ProfileConfiguration importProfile) throws FlockException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Mappable mappable = getMappable(importProfile);
        mappable.setData(row, importProfile);
        return (TagInputBean) mappable;

    }

    public static EntityInputBean transformToEntity(JsonNode node, ProfileConfiguration importProfile) throws FlockException {
        JsonEntityMapper entityInputBean = new JsonEntityMapper();
        entityInputBean.setData(node, importProfile);
        if (entityInputBean.getFortress() == null)
            entityInputBean.setFortress(importProfile.getFortressName());
        ContentInputBean contentInputBean = new ContentInputBean();
        if (contentInputBean.getFortressUser() == null)
            contentInputBean.setFortressUser(importProfile.getFortressUser());
        entityInputBean.setContent(contentInputBean);
        contentInputBean.setWhat(FlockDataJsonFactory.getObjectMapper().convertValue(node, Map.class));

        return entityInputBean;

    }

    public static EntityInputBean transformToEntity(XmlMappable mappable, XMLStreamReader xsr, ProfileConfiguration importProfile) throws FlockException, JAXBException, JsonProcessingException, IllegalAccessException, InstantiationException, ClassNotFoundException {

        XmlMappable row = mappable.newInstance(importProfile);
        ContentInputBean contentInputBean = row.setXMLData(xsr, importProfile);
        EntityInputBean entityInputBean = (EntityInputBean) row;

        if (entityInputBean.getFortress() == null)
            entityInputBean.setFortress(importProfile.getFortressName());

        if (entityInputBean.getFortressUser() == null)
            entityInputBean.setFortressUser(importProfile.getFortressUser());


        if (contentInputBean != null) {
            if (contentInputBean.getFortressUser() == null)
                contentInputBean.setFortressUser(importProfile.getFortressUser());
            entityInputBean.setContent(contentInputBean);
        }
        return entityInputBean;
    }
}
