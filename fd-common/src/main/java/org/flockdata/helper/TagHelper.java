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

package org.flockdata.helper;

import org.flockdata.dao.EntityTagDao;
import org.flockdata.model.EntityTag;
import org.flockdata.model.Tag;
import org.flockdata.registration.bean.TagInputBean;

/**
 * Created by mike on 20/06/15.
 */
public class TagHelper {

    public static String suffixLabel(String label, String tagSuffix) {
        if (label.startsWith(":"))
            label = label.substring(1);

        if ("".equals(tagSuffix))
            return label;
        return label + tagSuffix;
    }

    public static String parseKey(String key) {
        return key.toLowerCase();
    }

    public static boolean isDefault(String name) {
        return name == null || Tag.DEFAULT_TAG.equals(name) || Tag.DEFAULT.equals(name);
    }


    public static String parseKey(TagInputBean tagInput) {
        String prefix = (tagInput.getKeyPrefix() == null ? "" : tagInput.getKeyPrefix().toLowerCase() + ".");
        return prefix + tagInput.getCode().toLowerCase();
    }

    public static String parseKey(String keyPrefix, String tagCode) {
        if (keyPrefix == null)
            return tagCode.toLowerCase();
        return keyPrefix.toLowerCase() + "." + tagCode.toLowerCase();
    }

    public static boolean isSystemKey(String key) {
        boolean systemKey = false;

        if (key.equals(EntityTag.SINCE) || key.equals(EntityTagDao.FD_WHEN) || key.equals(Tag.LAT) || key.equals(Tag.LON))
            systemKey = true;
        return systemKey;


    }

    public static boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }
}
