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
import java.util.Objects;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.data.Model;
import org.flockdata.engine.data.graph.ModelNode;
import org.flockdata.model.ContentModelResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author mholdsworth
 * @tag neo4j, ContentModel
 * @tag Neo4j, ContentModel
 * @since 3/10/2014
 */
@Repository
public class ContentModelDaoNeo {

  private final ContentModelRepo contentModelRepo;

  private final Neo4jTemplate template;

  @Autowired
  public ContentModelDaoNeo(ContentModelRepo contentModelRepo, Neo4jTemplate template) {
    this.contentModelRepo = contentModelRepo;
    this.template = template;
  }


  public ModelNode findTagProfile(Company company, String code) {
    return contentModelRepo.findTagModel(company.getId(), code);
  }

  public ModelNode find(Fortress fortress, Document documentType) {
    return contentModelRepo.findTagModel(fortress.getId(), documentType.getId());
  }

  public Model save(Model modelToSave) {
    ModelNode model = contentModelRepo.save((ModelNode) modelToSave);
    template.fetch(model.getDocument());
    template.fetch(model.getFortress());
    return model;
  }

  public Collection<ContentModelResult> find(Long companyId) {
    Collection<ModelNode> models = contentModelRepo.findCompanyModels(companyId);
    Collection<ContentModelResult> results = new ArrayList<>(models.size());
    for (Model model : models) {
      if (model.getFortress() != null) {
        template.fetch(model.getFortress());
      }
      if (model.getDocument() != null) {
        template.fetch(model.getDocument());
      }
      results.add(new ContentModelResult(model));
    }
    return results;
  }

  public ContentModelResult findByKey(Long companyID, String key) {
    ModelNode model = contentModelRepo.findByKey(key);
    if (model == null) {
      return null;
    }

    if (!Objects.equals(model.getCompany().getId(), companyID)) {
      return null; // Somehow you have a key but it ain't for this company
    }

    // Profiles can simply be stored against the company if they just import tags
    if (model.getFortress() != null) {
      template.fetch(model.getFortress());
    }
    if (model.getDocument() != null) {
      template.fetch(model.getDocument());
    }
    if (model.getCompany() != null) {
      template.fetch(model.getCompany());
    }

    return new ContentModelResult(model);
  }

  public void delete(Company company, String key) {
    ModelNode model = contentModelRepo.findByKey(key);
    if (model == null) {
      return;
    }

    if (!Objects.equals(model.getCompany().getId(), company.getId())) {
      return; // Somehow you have a key but it ain't for this company
    }

    contentModelRepo.delete(model);

    // ToDo: delete the associated Entity
  }
}
