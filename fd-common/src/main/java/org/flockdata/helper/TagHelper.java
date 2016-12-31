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

package org.flockdata.helper;

import org.flockdata.data.EntityTag;
import org.flockdata.data.Tag;
import org.flockdata.registration.TagInputBean;

import java.util.ArrayList;

/**
 * Tag parsing support
 * <p/>
 * @author mholdsworth
 * @since 20/06/2015
 */
public class TagHelper {

    public static final String TAG = "Tag";

    public static String suffixLabel(String label, String tagSuffix) {
        if (label.startsWith(":"))
            label = label.substring(1);

        if ("".equals(tagSuffix))
            return label;
        return label + tagSuffix;
    }

    /**
     * Converts an incoming search string in to a format for storage as the Tag's key property.
     * <p/>
     * /'s and .'s are converted to a - to avoid ambiguity with URI paths so can be found as /, - or %2F.
     * There is an assumption that the incoming key could be a URL string that requires decoding.
     *
     * @param key raw incoming text
     * @return lowercase, url decoded string with /'s converted to -'s
     */
    public static String parseKey(String key) {
        String result = key.toLowerCase();
        return result.replace('/', '-');

    }

    public static boolean isDefault(String name) {
        return name == null || Tag.DEFAULT_TAG.equals(name) || Tag.DEFAULT.equals(name);
    }


    public static String parseKey(TagInputBean tagInput) {
        //String prefix = (tagInput.getKeyPrefix() == null ? "" : tagInput.getKeyPrefix().toLowerCase() + "-");
        return  parseKey(tagInput.getKeyPrefix(), tagInput.getCode());
    }

    public static String parseKey(String keyPrefix, String tagCode) {
        if (keyPrefix == null)
            return parseKey(tagCode);
        return keyPrefix.toLowerCase() + "-" + parseKey(tagCode);
    }

    public static boolean isSystemKey(String key) {
        boolean systemKey = false;

        if (key.equals(EntityTag.SINCE) || key.equals(EntityTag.FD_WHEN) || key.equals(Tag.LAT) || key.equals(Tag.LON))
            systemKey = true;
        return systemKey;


    }

    public static boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }

    public static String getLabel(ArrayList<String> labels) {
        for (String label : labels) {
            if (!NodeHelper.isInternalLabel(label))
                return label;
        }
        return TAG;

    }
}
