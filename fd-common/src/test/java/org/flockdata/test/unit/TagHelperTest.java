package org.flockdata.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagHelper;
import org.junit.jupiter.api.Test;

/**
 * @author mikeh
 * @since 2018-11-19
 */
class TagHelperTest {

    @Test
    void nullDefaults() {
        Tag nullTag = null;
        assertThat(TagHelper.isDefault(nullTag)).isFalse();
        assertThat(TagHelper.isDefault((String) null)).isFalse();
    }

    @Test
    void nonDefaultLabel() {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(Tag.DEFAULT_TAG);
        labels.add("NonDefault");
        assertThat(TagHelper.getLabel(labels)).isEqualToIgnoringCase("NonDefault");
    }

    @Test
    void whenOnlyLabelThenDefaultIsReturned() {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(Tag.DEFAULT_TAG);
        assertThat(TagHelper.getLabel(labels)).isEqualToIgnoringCase(Tag.DEFAULT_TAG);
    }

}
