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

package org.flockdata.test.client;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.csv.CsvTagMapper;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
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
        ImportProfile params = ClientConfiguration.getImportParams("/tag-expressions.json");
        CsvTagMapper mapper = new CsvTagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(headers, data, params, reader);
        assertNotNull(json);
        assertNotNull(mapper);
        assertEquals("Politician", mapper.getLabel());
        assertEquals("Whitehouse, Sheldon", mapper.getName());
        assertEquals("01823", mapper.getCode());
        assertEquals(1, mapper.getTargets().get("HAS_ALIAS").size());
    }

    @Test
    public void string_Properties() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/tag-expressions.json");
        CsvTagMapper mapper = new CsvTagMapper();
        String[] headers = new String[]{"last_name", "first_name", "birthday", "gender", "type", "state", "district", "party", "url", "address", "phone", "contact_form", "rss_url", "twitter", "facebook", "facebook_id", "youtube", "youtube_id", "bioguide_id", "thomas_id", "opensecrets_id", "lis_id", "cspan_id", "govtrack_id", "votesmart_id", "ballotpedia_id", "washington_post_id", "icpsr_id", "wikipedia_id"};
        String[] data = new String[]{"Whitehouse", "Sheldon", "1955-10-20", "M", "sen", "RI", "", "Democrat", "http://www.whitehouse.senate.gov", "530 Hart Senate Office Building Washington DC 20510", "202-224-2921", "http://www.whitehouse.senate.gov/contact", "http://www.whitehouse.senate.gov/rss/feeds/?type=all&amp;cachebuster=1", "SenWhitehouse", "SenatorWhitehouse", "194172833926853", "SenatorWhitehouse", "UCnG0N70SNBkNqvIMLodPTIA", "W000802", "01823", "N00027533", "S316", "92235", "412247", "2572", "Sheldon Whitehouse", "gIQA7KHw9O", "40704", "Sheldon Whitehouse"};
        Map<String, Object> json = mapper.setData(headers, data, params, reader);
        assertNotNull(json);
        assertNotNull(mapper);

        assertEquals("Custom properties not being set", 3, mapper.getProperties().size());
        boolean birthdaySet = false, urlSet = false, genderSet = false;
        for (String key : mapper.getProperties().keySet()) {
            if (key.equals("dob")) {
                assertEquals("1955-10-20", mapper.getProperties().get("dob"));
                birthdaySet = true;
            } else if (key.equals("url")) {
                urlSet = true;
                assertEquals("http://www.whitehouse.senate.gov", mapper.getProperties().get("url"));
            } else if (key.equals("gender"))
                genderSet = true;
        }

        assertEquals("Unable to find remapped target property name", true, birthdaySet);
        assertEquals(true, urlSet);
        assertEquals(true, genderSet);
    }

    @Test
    public void label_expressionsAndConstants() throws Exception {
        ClientConfiguration configuration= getClientConfiguration("/tag-label-expressions.json");
        FileProcessor fileProcessor = new FileProcessor(reader);
        fileProcessor.processFile(ClientConfiguration.getImportParams("/tag-label-expressions.json"),
                "/tag-label-expressions.csv", 0, fdWriter, null, configuration);

    }

    static FdWriter fdWriter = new FdWriter() {
        @Override
        public SystemUserResultBean me() {
            return null;
        }

        @Override
        public  String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            // 1 Politician
            //
            assertEquals(4, tagInputBeans.size());
            for (TagInputBean tagInputBean : tagInputBeans) {
                if ( tagInputBean.getLabel().equals("Agency"))
                    assertEquals("1", tagInputBean.getCode());
                else if ( tagInputBean.getLabel().equals("Edit Status"))
                    assertEquals("7", tagInputBean.getCode());
                else if ( tagInputBean.getLabel().equals("MSA/MD"))
                    assertEquals("10180", tagInputBean.getCode());
                else if ( tagInputBean.getLabel().equals("County"))
                    assertEquals("9", tagInputBean.getCode());
                else
                    throw new FlockException("Unexpected tag - " + tagInputBean.toString());

            }
            return null;
        }

        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            return null;
        }

        @Override
        public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
            return 0;
        }

        @Override
        public boolean isSimulateOnly() {
            // Setting this to true will mean that the flush routines above are not called
            return false;
        }

        @Override
        public Collection<Tag> getCountries() throws FlockException {
            return null;
        }

        @Override
        public void close() {

        }
    };

}
