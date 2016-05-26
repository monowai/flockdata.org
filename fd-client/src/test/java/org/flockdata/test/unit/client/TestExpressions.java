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

package org.flockdata.test.unit.client;

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.transform.ProfileReader;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.tags.TagMapper;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * SPeL tests and custom properties for tags
 * <p>
 * Created by mike on 17/01/15.
 */
public class TestExpressions extends AbstractImport {
    @Test
    public void string_Concatenation() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/tag-expressions.json");
        TagMapper mapper = new TagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(json);
        assertNotNull(mapper);
        assertEquals("Politician", mapper.getLabel());
        assertEquals("Whitehouse, Sheldon", mapper.getName());
        assertEquals("01823", mapper.getCode());
        assertEquals(1, mapper.getTargets().get("HAS_ALIAS").size());
    }

    @Test
    public void string_Properties() throws Exception {
        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/tag-expressions.json");
        TagMapper mapper = new TagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(Transformer.convertToMap(headers, data, params), params);
        assertNotNull(json);
        assertNotNull(mapper);

        assertEquals("Custom properties not being set", 3, mapper.getProperties().size());
        boolean birthdaySet = false, urlSet = false, genderSet = false;
        for (String key : mapper.getProperties().keySet()) {
            switch (key) {
                case "dob":
                    assertEquals("1955-10-20", mapper.getProperties().get("dob"));
                    birthdaySet = true;
                    break;
                case "url":
                    urlSet = true;
                    assertEquals("http://www.whitehouse.senate.gov", mapper.getProperties().get("url"));
                    break;
                case "gender":
                    genderSet = true;
                    break;
            }
        }

        assertEquals("Unable to find remapped target property name", true, birthdaySet);
        assertEquals(true, urlSet);
        assertEquals(true, genderSet);
    }


}
