/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.helper;

import org.flockdata.dao.EntityTagDao;
import org.flockdata.model.EntityTag;
import org.flockdata.model.Tag;
import org.flockdata.registration.TagInputBean;
import org.neo4j.graphdb.Label;

/**
 * Tag parsing support
 * <p/>
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

        if (key.equals(EntityTag.SINCE) || key.equals(EntityTagDao.FD_WHEN) || key.equals(Tag.LAT) || key.equals(Tag.LON))
            systemKey = true;
        return systemKey;


    }

    public static boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }

    public static boolean isInternalLabel(String label) {
        if (label.equals("_Tag") || label.equals("Tag"))
            return true;
        return false;
    }

    public static String getLabel(Iterable<Label> labels) {
        for (Label label : labels) {
            if (!isInternalLabel(label.name()))
                return label.name();
        }
        return "Tag";
    }
}
