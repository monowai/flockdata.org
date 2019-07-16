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

package org.flockdata.test.engine.services;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.data.EntityTag;
import org.flockdata.data.SystemUser;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Geo Tag tests
 *
 * @author mholdsworth
 * @since 6/11/2015
 */
public class TestGeoEntity extends EngineBase {

    private static final String ZIP = "90210";
    private static final String THE_LENDER = "TheLender";

    @Override
    @Before
    public void cleanUpGraph() {
        // DAT-348
        super.cleanUpGraph();
    }

    /**
     * Checks that a custom query can be used to return a Geographic node path rather than
     * having to use FlockData's internal default resolution strategy
     * <p>
     * DAT-495 introduces this
     *
     * @throws Exception
     */
    @Test
    public void geo_CustomPath() throws Exception {
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentNode documentType = new DocumentNode(fortress, "DAT-495");

        documentType = conceptService.save(documentType);
        assertNull(documentType.getGeoQuery());

        // DocumentType specific query string to define how the geo chain is connected
        String query = "match p=(located:Tag)-[*1..2]->(x:Country)  where id(located)={locNode} return nodes(p) as nodes";
        documentType.setGeoQuery(query);
        conceptService.save(documentType);
        documentType = conceptService.findDocumentType(fortress, documentType.getName());
        assertNotNull(documentType);
        assertEquals(query, documentType.getGeoQuery());

        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT-495", "DAT-495", new DateTime(), "abc");

        TagInputBean tagInput = new TagInputBean("123 Main Road", "Address", new EntityTagRelationshipInput("geodata", true));
        tagInput.setTargets("to-country", new TagInputBean("AT", "Country").setName("Atlantis"));

        Collection<TagInputBean> tags = new ArrayList<>();
        tags.add(tagInput);
        entityInput.setTags(tags);
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Iterable<EntityTag> entityTags = entityTagService.findEntityTagsWithGeo(trackResultBean.getEntity());
        int expected = 1;
        int found = 0;
        for (EntityTag entityTag : entityTags) {
            assertNotNull("custom geo query string did not find the geo path", entityTag.getGeoData());
            found++;
        }
        assertEquals(expected, found);
    }

    @Test
    public void tag_entityCustomGeo() throws Exception {
        // DAT-508
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true)
            .setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentNode documentType = new DocumentNode(fortress, "DAT-508");

        documentType = conceptService.save(documentType);
        assertNull(documentType.getGeoQuery());

        // DocumentType specific query string to define how the geo chain is connected
        String query = "match p=(located:Tag)-[r:state]->(o)-[*1..3]->(x:Country)  where id(located)={locNode} return nodes(p) as nodes";
//        documentType.setGeoQuery(query);
        conceptService.save(documentType);
        documentType = conceptService.findDocumentType(fortress, documentType.getName());
        assertNotNull(documentType);
//        assertEquals(query, documentType.getGeoQuery());

        // Invalid zip code structure connected to Texas
        TagInputBean zipInvalid = new TagInputBean(ZIP, "ZipCode");
        TagInputBean state = new TagInputBean("TX", "State");
        TagInputBean country = new TagInputBean("US", "Country");

        zipInvalid.setTargets("state", state);
        state.setTargets("country", country);

        tagService.createTag(su.getCompany(), zipInvalid);

        // Valid zip structure connected to the California
        TagInputBean zipValid = new TagInputBean(ZIP, "ZipCode");
        state = new TagInputBean("CA", "State");
        zipValid.setTargets("state", state);
        state.setTargets("country", country);

        tagService.createTag(su.getCompany(), zipValid);

        TagInputBean countyTag = new TagInputBean("Los Angeles", "County")
            .setTargets("state", state);

        TagInputBean lender = new TagInputBean(THE_LENDER, "Lender");
        // Very specific scenario. The lender is incorrectly set to the same zipcode
        // which in turns contains both a valid state relationship and an invalid one.
        lender.setTargets("located", new TagInputBean(ZIP, "ZipCode"));

        Tag lenderTag = tagService.createTag(su.getCompany(), lender).getTag();
        // Entity->Lender->Zip->State->Country

        TestCase.assertEquals("Lender should be connected to one Zip", 1, tagService.findDirectedTags(lenderTag).size());
        TestCase.assertEquals("That zip should be connected to two states", 2, tagService.findDirectedTags(tagService.findDirectedTags(lenderTag).iterator().next()).size());

        Map<String, Collection<FdTagResultBean>> subTags = tagService.findTags(su.getCompany(), "ZipCode", ZIP, "state", "State");
        assertTrue(subTags.containsKey("state"));
        assertEquals("The zip code should be connected to two states", 2, subTags.get("state").size());

        EntityInputBean entityInput =
            new EntityInputBean(fortress, "DAT-508", documentType.getName(), new DateTime(), "abc")
                .addTag(lender
                    .addEntityTagLink(new EntityTagRelationshipInput("created", true)))
                .addTag(countyTag
                    .addEntityTagLink(new EntityTagRelationshipInput("in-county", true)));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);

        // The Lender is connected two a Zip code that has two different paths back to a country one via TX the other via CA
        // This is illegal data in the real world and makes accessing the Lender "located" information ambiguous.
        //
        // Using "out-of-the-box" functionality we would get an exception
        // NoSuchElementException: More than one element....

        // However, we are retrieving the geo structure via a custom query set in the DocType, ignoring
        // the invalid data relationship to find only one geodata path associated with the County

        searchService.getEntityChange(result);

        Iterable<EntityTag> iTags = entityTagService.findEntityTagsWithGeo(result.getEntity());
        int count = 0;
        for (EntityTag entityTag : iTags) {
            assertNotNull(entityTag.getGeoData());
            count++;
        }
        assertEquals("expected a Lender and a County relationship", 2, count);
    }

    @Test
    public void geo_Relationship() throws Exception {
        SystemUser su = registerSystemUser("geo_Relationship", mike_admin);
        FortressInputBean fib = new FortressInputBean("geo_Relationship", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentNode documentType = new DocumentNode(fortress, "geo_Relationship");

        documentType = conceptService.save(documentType);
        assertNull(documentType.getGeoQuery());

        EntityInputBean entityInput = new EntityInputBean(fortress, "geo_Relationship", "geo_Relationship", new DateTime(), "abc");

        TagInputBean address = new TagInputBean("123 Main Road", "Address");

        TagInputBean atlantis = new TagInputBean("AT", "Country")
            .setName("Atlantis")
            .setProperty(TagNode.LAT, 24.8236183)
            .setProperty(TagNode.LON, -75.5058183);

        address.setTargets("links-to-country", atlantis);
        address.addEntityTagLink(new EntityTagRelationshipInput("via-address", true));
        atlantis.addEntityTagLink(new EntityTagRelationshipInput("in-country", true));

        Collection<TagInputBean> tags = new ArrayList<>();
        tags.add(address);
        tags.add(atlantis);
        entityInput.setTags(tags);
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Iterable<EntityTag> entityTags = entityTagService.findEntityTagsWithGeo(trackResultBean.getEntity());
        int expected = 2;
        int found = 0;
        for (EntityTag entityTag : entityTags) {
            assertNotNull("Geo relationship did not resolve from the setGeo flag", entityTag.getGeoData());
            found++;
        }
        assertEquals(expected, found);
    }
}
