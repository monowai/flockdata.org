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

package org.flockdata.engine.dao;

import org.flockdata.authentication.registration.bean.AliasInputBean;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:31 PM
 */
@Repository
public class AliasDaoNeo {
    @Autowired
    AliasRepo aliasRepo;

    @Autowired
    Neo4jTemplate template;


    public Collection<Alias> findTagAliases ( Tag tag ){
        return aliasRepo.findTagAliases(tag.getId());
    }

    public Alias findAlias(String label, AliasInputBean newAlias, Tag startTag) {
        Alias alias = null;
        String key = TagHelper.parseKey(newAlias.getCode());
        String query = "match (a:`" + label + "Alias` {key:{key}}) return a";
        Map<String,Object> params = new HashMap<>();
        params.put("key", key);
        Result<Map<String, Object>> dbResults = template.query(query, params);
        Iterator<Map<String, Object>> results = dbResults.iterator();
        while (results.hasNext()) {
            Map<String, Object> mapResult = results.next();
            alias = template.projectTo(mapResult.get("a"), Alias.class);
        }

        if ( alias == null )
            alias = new Alias(label, newAlias, key, startTag);

        return alias;
    }
}
