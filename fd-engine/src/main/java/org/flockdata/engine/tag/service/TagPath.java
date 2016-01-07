package org.flockdata.engine.tag.service;

import org.flockdata.engine.tag.dao.TagPathDao;
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
 * Created by mike on 28/12/15.
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
