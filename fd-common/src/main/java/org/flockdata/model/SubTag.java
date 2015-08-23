package org.flockdata.model;

import java.util.Map;

/**
 * Created by mike on 22/08/15.
 */
public class SubTag extends EntityTag {
    Long id ;
    Tag tag;
    String relationship;

    public SubTag() {}

    public SubTag(Tag subTag, String label) {
        this.tag = subTag;
        this.relationship = label;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String getRelationship() {
        return relationship;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return null;
    }

    @Override
    public Boolean isReversed() {
        return false;
    }

    @Override
    public void setRelationship(String name) {
        this.relationship = name;
    }
}
