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

package org.flockdata.transform.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.model.PayloadTransformer;

/**
 * Transforms the Map in to a suitable TagInput payload
 *
 * @author mholdsworth
 * @tag Tag, ContentModel, Transformer, Helper
 * @since 27/04/2014
 */
public class TagPayloadTransformer implements PayloadTransformer {

  private final ContentModel contentModel;
  private Collection<TagInputBean> tags = new ArrayList<>();

  public TagPayloadTransformer(ContentModel contentModel) {
    this.contentModel = contentModel;
  }

  public static TagPayloadTransformer newInstance(ContentModel contentModel) {
    return new TagPayloadTransformer(contentModel);
  }

  public Map<String, Object> transform(Map<String, Object> row) throws FlockException {
    return transform(row, contentModel);
  }

  protected Map<String, Object> transform(Map<String, Object> row, ContentModel contentModel) throws FlockException {
    if (!TransformationHelper.processRow(row, contentModel)) {
      return null;
    }

    Map<String, ColumnDefinition> content = contentModel.getContent();

    for (String column : content.keySet()) {
      ColumnDefinition colDef = content.get(column);
      String value;
      Object colValue = row.get(column);
      // colValue may yet be an expression
      value = (colValue != null ? colValue.toString() : null);
      if (value != null) {
        value = value.trim();
      }

      if (colDef != null && colDef.isTag()) {

        TagInputBean tagInputBean = new TagInputBean();

        TransformationHelper.setTagInputBean(tagInputBean, row, column, contentModel, value);
        if (TransformationHelper.evaluate(colDef.isTitle())) {
          tagInputBean.setName(ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, value));
          if (colDef.getCode() != null) {
            row.get(colDef.getCode());
          }
        }

        if (colDef.getLabelDescription() != null) {
          value = ExpressionHelper.getValue(row, colDef.getLabelDescription(), colDef, row.get(column));
          Object oValue = ExpressionHelper.getValue(value, colDef);
          if (oValue != null) {
            tagInputBean.setDescription(oValue.toString());
          }

        }
        if (colDef.getTarget() != null && TransformationHelper.evaluate(colDef.isPersistent(), true)) {
          value = ExpressionHelper.getValue(row, colDef.getValue(), colDef, row.get(column));
          Object oValue = ExpressionHelper.getValue(value, colDef);
          if (oValue != null) {
            tagInputBean.setProperty(colDef.getTarget(), oValue);
          }
        }
        if (colDef.getGeoData() != null) {
          TransformationHelper.doGeoTransform(tagInputBean, row, colDef);
        }
        tags.add(tagInputBean);

      } // ignoreMe
    }
    return row;
  }

  public Collection<TagInputBean> getTags() {
    return tags;
  }
}
