package org.flockdata.engine.track.service;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.EntityTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Encapsulates the functionality to return default entity tags
 *
 * Created by mike on 24/08/15.
 */
@Repository
public class DefaultTagFinder implements EntityTagFinder {

    @Autowired
    EntityTagService entityTagService;

    @Override
    public Iterable<EntityTag> getEntityTags(Entity entity) {
        return entityTagService.getEntityTagsWithGeo(entity);
    }

    @Override
    public EntityService.TAG_STRUCTURE getTagStructure() {
        return EntityService.TAG_STRUCTURE.DEFAULT;
    }
}
