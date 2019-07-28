/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.data.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.flockdata.data.Alias;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.AliasNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.AliasInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author mholdsworth
 * @tag Neo4j, Alias
 * @since 3/10/2014
 */
@Repository
public class AliasDaoNeo {
  private final AliasRepo aliasRepo;

  private final Neo4jTemplate template;

  @Autowired
  public AliasDaoNeo(AliasRepo aliasRepo, Neo4jTemplate template) {
    this.aliasRepo = aliasRepo;
    this.template = template;
  }


  public Collection<Alias> findTagAliases(Tag tag) {
    return aliasRepo.findTagAliases(tag.getId());
  }

  public AliasNode findAlias(String label, AliasInputBean newAlias, TagNode startTag) {
    AliasNode alias = null;
    String key = TagHelper.parseKey(newAlias.getCode());
    String query = "match (a:`" + label + "Alias` {key:{key}}) return a";
    Map<String, Object> params = new HashMap<>();
    params.put("key", key);
    Result<Map<String, Object>> dbResults = template.query(query, params);
    Iterator<Map<String, Object>> results = dbResults.iterator();
    while (results.hasNext()) {
      Map<String, Object> mapResult = results.next();
      alias = template.projectTo(mapResult.get("a"), AliasNode.class);
    }

    if (alias == null) {
      alias = new AliasNode(label, newAlias, key, startTag);
    }

    return alias;
  }
}
