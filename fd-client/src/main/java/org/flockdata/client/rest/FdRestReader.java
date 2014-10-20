/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.client.rest;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.FdWriter;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 8/07/14
 * Time: 10:10 AM
 */
public class FdRestReader implements FdReader {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdRestReader.class);
    Map<String, TagInputBean> countriesByName = new HashMap<>();

    private FdWriter writer;

    public FdRestReader(FdWriter writer) {
        setWriter(writer);
    }

    public void setWriter(FdWriter writer) {
        this.writer = writer;
    }

    /**
     * resolves the country Name to an ISO code
     *
     * @param name long name of the country
     * @return iso code to use
     */
    @Override
    public String resolveCountryISOFromName(String name) throws FlockException {
//        if (simulateOnly)
//            return name;

        // 2 char country? it's already ISO
        if (name.length() == 2)
            return name;

        if (countriesByName.isEmpty() ) {

            Collection countries = writer.getCountries();
            countriesByName = new HashMap<>(countries.size());
            for (Object country : countries) {
                TagInputBean simpleTag = new TagInputBean();
                TagInputBean tag = (TagInputBean) country;
                simpleTag.setCode(tag.getCode());
                simpleTag.setName(tag.getName());
                countriesByName.put(tag.getName().toLowerCase(), tag);
            }

        }
        TagInputBean tag = countriesByName.get(name.toLowerCase());
        if (tag == null) {
            logger.error("Unable to resolve country name [{}]", name);
            return null;
        }
        return tag.getCode();
    }

    @Override
    public String resolve(String type, Map<String, Object> args) {
        return null;
    }

}
