package org.flockdata.test.helper;

import junit.framework.TestCase;
import org.flockdata.registration.bean.TagInputBean;
import org.junit.Test;

/**
 * Created by mike on 5/08/15.
 */
public class TestTagInputBean {
    @Test
    public void duplicateTargets() throws Exception {
        TagInputBean root = new TagInputBean("root", "RootTag");
        TagInputBean tagA = new TagInputBean("taga", "Label");
        TagInputBean tagB = new TagInputBean("tagb", "Label");
        TagInputBean subTag = new TagInputBean("sub", "SubTag");

        tagA.setTargets("other", subTag);
        root.setTargets("rlx", tagA);

        tagB.setTargets("other", subTag);
        root.setTargets("rlx", tagB);

        boolean aFound = false;
        boolean bFound = false;

        TestCase.assertEquals("Tag was not accumulated", 2, root.getTargets().get("rlx").size());
        for (TagInputBean tagInputBean : root.getTargets().get("rlx")) {
            switch (tagInputBean.getCode()) {
                case "taga":
                    TestCase.assertFalse(aFound);
                    TestCase.assertNotNull(tagInputBean.getTargets().get("other"));
                    aFound = true; // Make sure each tag is found uniquely once
                    break;
                case "tagb":
                    TestCase.assertFalse(bFound);
                    TestCase.assertNotNull(tagInputBean.getTargets().get("other"));
                    bFound = true;
                    break;
                default:
                    throw new RuntimeException("Unexpected tag " + tagInputBean);
            }
        }

        root.setTargets("rlx", tagB);
        TestCase.assertEquals("Duplicate tag should not be added. Equality check fails?", 2, root.getTargets().get("rlx").size());

    }

    @Test
    public void nestedSubTags() throws Exception {
        TagInputBean interest = new TagInputBean("Entertainment", "Interest");
        TagInputBean art = new TagInputBean("art", "Category");
        TagInputBean architecture = new TagInputBean("architecture", "Division");

        TagInputBean termA = new TagInputBean("Ancient Roman", "Term");
        TagInputBean termB = new TagInputBean("Architect", "Term");

        interest.setTargets("is", art);
        art.setTargets("typed", architecture);
        architecture.setTargets("classifying", termA);
        architecture.setTargets("classifying", termB);

        TestCase.assertEquals(1, interest.getTargets().get("is").size());
        TagInputBean temp = interest.getTargets().get("is").iterator().next();
        TestCase.assertEquals(1, temp.getTargets().get("typed").size());
        TestCase.assertEquals(2, temp.getTargets().get("typed").iterator().next().getTargets().get("classifying").size());

    }

    @Test
    public void mixedCase() throws Exception {
        TagInputBean root = new TagInputBean("root", "RootTag");
        // These should be treated as the same tag
        TagInputBean tagA = new TagInputBean("tagA", "Label");
        TagInputBean tagB = new TagInputBean("taga", "Label");

        root.setTargets("rlx", tagA);

        root.setTargets("rlx", tagB);

        TestCase.assertEquals("Duplicate Tag was not detected", 1, root.getTargets().get("rlx").size());

    }

    @Test
    public void containsTargetTag() throws Exception {
        // Exercise the .contains functionality DAT-491
        TagInputBean root = new TagInputBean("cricket", "Sport");
        TagInputBean players = new TagInputBean("players", "Division").setKeyPrefix("abc");
        TagInputBean playerx = new TagInputBean("players", "Division").setKeyPrefix("whee");
        TagInputBean league = new TagInputBean("icc", "Division");
        TagInputBean india = new TagInputBean("india", "Division");
        TagInputBean nz = new TagInputBean("new zealand", "Division");

        root.setTargets("blah", players);
        root.setTargets("twee", playerx);
        players.setTargets("wah", league);
        league.setTargets("xxx", india);
        league.setTargets("xxx", nz);
        TagInputBean emptyTag = new TagInputBean("abc", "123");
        TestCase.assertFalse(emptyTag.contains("abc", "123"));

        TestCase.assertTrue(root.contains("players", "Division", "abc"));
        TestCase.assertTrue(root.contains("players", "Division", "whee"));
        TestCase.assertTrue(root.contains("india", "Division", null));
        TagInputBean foundTag = root.findTargetTag("players", "Division", "whee");
        TestCase.assertEquals("whee", foundTag.getKeyPrefix());
        foundTag = root.findTargetTag("players", "Division", "abc");
        TestCase.assertEquals("abc", foundTag.getKeyPrefix());
        foundTag = root.findTargetTag("players", "Division", null);
        TestCase.assertNotNull("Undefined as to which one we find because no keyPrefix is provided", foundTag);

    }
}
