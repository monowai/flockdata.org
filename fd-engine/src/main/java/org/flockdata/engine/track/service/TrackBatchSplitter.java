/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.engine.track.service;

import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 *
 * Created by mike on 21/03/15.
 */
@Service
public class TrackBatchSplitter {
    @Autowired
    FortressService fortressService;

    /**
     * inputs is modified such that it will only contain existing TrackResult entities
     *
     * @param inputs all entities to consider - this collection is modified
     * @return Entities from inputs that are new
     */
    public static Collection<TrackResultBean> getNewEntities(Collection<TrackResultBean> inputs){
        Collection<TrackResultBean> newEntities = new ArrayList<>();
        for (TrackResultBean track : inputs) {
            if (track.getEntity().isNewEntity()) {
                newEntities.add(track);
            }
        }
        return newEntities;
    }

    public static Collection<TrackResultBean>  getExistingEntities(Collection<TrackResultBean> inputs) {
        Collection<TrackResultBean> newEntities = new ArrayList<>();
        for (TrackResultBean track : inputs) {
            if (!track.getEntity().isNewEntity()) {
                newEntities.add(track);
            }
        }
        return newEntities;

    }

    public  Map<Fortress, List<EntityInputBean>> getEntitiesByFortress(Company company, Collection<EntityInputBean> entityInputBeans) throws NotFoundException {
        Map<Fortress, List<EntityInputBean>> results = new HashMap<>();

        // Local cache of fortress by name - never very big, usually only 1
        Map<String, Fortress> resolvedFortress = new HashMap<>();
        for (EntityInputBean entityInputBean : entityInputBeans) {
            String fortressName = entityInputBean.getFortress();
            Fortress f = resolvedFortress.get(fortressName.toLowerCase());
            if (f == null) {
                f = fortressService.findByCode(company, fortressName);
                if ( f== null)
                    f = fortressService.findByName(company, fortressName);
            }

            if (f != null)
                resolvedFortress.put(fortressName.toLowerCase(), f);

            List<EntityInputBean> entities = null;
            if (f != null)
                entities = results.get(f);// are we caching this already?

            if (entities == null) {
                entities = new ArrayList<>();
                if ( f == null ) {
                    FortressInputBean fib = new FortressInputBean(fortressName);
                    fib.setTimeZone(entityInputBean.getTimezone());
                    f = fortressService.registerFortress(company, fib, true);
                }
                resolvedFortress.put(fortressName.toLowerCase(), f);
                results.put(f, entities);
            }
            entities.add(entityInputBean);
        }
        return results;
    }

}
