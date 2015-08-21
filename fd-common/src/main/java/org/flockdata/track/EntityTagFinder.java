package org.flockdata.track;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;

/**
 * Classes that implement this can find and massage the EntityTag objects that will be
 * sent for indexing in fd-search
 *
 * Created by mike on 20/08/15.
 */
public interface EntityTagFinder {

    Iterable<EntityTag> getEntityTags(Entity entity);
}
