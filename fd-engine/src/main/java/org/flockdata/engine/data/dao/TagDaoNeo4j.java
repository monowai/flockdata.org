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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Alias;
import org.flockdata.data.Company;
import org.flockdata.data.Tag;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.TagPayload;
import org.flockdata.track.bean.FdTagResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Wrap calls to Neo4j server extension. Figuring this out....
 *
 * @author mholdsworth
 * @tag Neo4j, Tag
 * @since 29/06/2013
 */
@Repository
public class TagDaoNeo4j {

  private final TagWrangler tagWrangler;

  private final AliasDaoNeo aliasDaoNeo;

  @Autowired
  public TagDaoNeo4j(TagWrangler tagWrangler, AliasDaoNeo aliasDaoNeo) {
    this.tagWrangler = tagWrangler;
    this.aliasDaoNeo = aliasDaoNeo;
  }

  public Collection<FdTagResultBean> save(TagPayload payload) {
    return tagWrangler.save(payload);
  }

  public void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
    tagWrangler.createAlias(suffix, tag, label, aliasInput);
  }

  public Collection<Tag> findDirectedTags(String tagSuffix, Tag startTag, Company company) {
    return tagWrangler.findDirectedTags(tagSuffix, startTag, company);
  }

  public Collection<Tag> findTags(String label) {
    return tagWrangler.findTags(label);
  }

  public Collection<TagResultBean> findTags() {
    return tagWrangler.findTags();
  }

  public Collection<AliasInputBean> findTagAliases(Tag sourceTag) throws NotFoundException {
    Collection<Alias> aliases = aliasDaoNeo.findTagAliases(sourceTag);
    Collection<AliasInputBean> aliasResults = new ArrayList<>();
    for (Alias alias : aliases) {
      aliasResults.add(new AliasInputBean(alias.getName()));
    }
    return aliasResults;
  }

  public Map<String, Collection<FdTagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
    return tagWrangler.findAllTags(sourceTag, relationship, targetLabel);

  }

  public Tag findTagNode(String suffix, String label, String tagPrefix, String tagCode, boolean inflate) {
    return tagWrangler.findTag(suffix, label, tagPrefix, tagCode, inflate);
  }


//    public Collection<Tag> findTags(String suffix, String label, String tagCode, boolean inflate) {
//        return tagWrangler.findTags(TagHelper.suffixLabel(label, suffix), tagCode);
//    }
}
