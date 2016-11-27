/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.engine.tag.service;

import org.flockdata.engine.dao.TagPathDao;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.track.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 28/12/2015
 */
@Service
@Transactional
public class TagPath {

    @Autowired
    private TagPathDao tagPathDao;

    @Autowired
    private TagService tagService;

    private Logger logger = LoggerFactory.getLogger(TagPath.class);

    public Collection<Map<String, Object>> getPaths(Company company, String label, String code, int length, String targetLabel) throws NotFoundException {
        Tag tag = tagService.findTag(company, label, null, code, false);
        if ( length <1 )
            length = 4;
        return tagPathDao.getPaths(tag, length, targetLabel);
    }
}
