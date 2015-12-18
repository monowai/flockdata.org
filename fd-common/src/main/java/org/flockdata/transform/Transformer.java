package org.flockdata.transform;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.csv.CsvEntityMapper;
import org.flockdata.transform.tags.TagMapper;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

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
}
