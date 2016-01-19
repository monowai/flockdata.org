package org.flockdata.test.engine.functional;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.flockdata.model.*;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by mike on 6/11/15.
 */
public class TestGeoEntity extends EngineBase {

    public static final String ZIP = "90210";
    public static final String THE_LENDER = "TheLender";

    @Override
    @Before
    public void cleanUpGraph() {
        // DAT-348
        super.cleanUpGraph();
    }

    /**
     * Checks that a custom query can be used to return a Geographic node path rather than
     * having to use FlockData's internal default resolution strategy
     *
     * DAT-495 introduces this
     *
     * @throws Exception
     */
    @Test
    public void geo_CustomPath() throws Exception {
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true);
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentType documentType = new DocumentType(fortress, "DAT-495");

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

        TagInputBean tagInput = new TagInputBean("123 Main Road", "Address", "geodata");
        tagInput.setTargets("to-country", new TagInputBean("AT", "Country").setName("Atlantis"));

        Collection<TagInputBean> tags = new ArrayList<>();
        tags.add(tagInput);
        entityInput.setTags(tags);
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Iterable<EntityTag> entityTags = entityTagService.getEntityTagsWithGeo(trackResultBean.getEntity());
        int expected = 1;
        int found = 0;
        for (EntityTag entityTag : entityTags) {
            assertNotNull ( "custom geo query string did not find the geo path", entityTag.getGeoData());
            found ++;
        }
        assertEquals(expected, found);
    }

    @Test
    public void tag_entityCustomGeo () throws Exception {
        // DAT-508
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true)
                .setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentType documentType = new DocumentType(fortress, "DAT-508");

        documentType = conceptService.save(documentType);
        assertNull(documentType.getGeoQuery());

        // DocumentType specific query string to define how the geo chain is connected
        String query = "match p=(located:Tag)-[r:state]->(o)-[*1..3]->(x:Country)  where id(located)={locNode} return nodes(p) as nodes";
        documentType.setGeoQuery(query);
        conceptService.save(documentType);
        documentType = conceptService.findDocumentType(fortress, documentType.getName());
        assertNotNull(documentType);
        assertEquals(query, documentType.getGeoQuery());

        // Invalid zip code structure connected to Texas
        TagInputBean zipInvalid = new TagInputBean(ZIP, "ZipCode");
        TagInputBean state   = new TagInputBean("TX", "State");
        TagInputBean country = new TagInputBean("US", "Country");

        zipInvalid.setTargets("state", state);
        state.setTargets("country", country);

        Tag tagInvalid = tagService.createTag(su.getCompany(), zipInvalid);

        // Valid zip structure connected to the California
        TagInputBean zipValid = new TagInputBean(ZIP, "ZipCode");
        state = new TagInputBean("CA", "State");
        zipValid.setTargets("state", state);
        state.setTargets("country", country);

        tagService.createTag(su.getCompany(), zipValid);

        TagInputBean countyTag =new TagInputBean("Los Angeles", "County")
                .setEntityLink("geodata")
                .setTargets("state", state);

        TagInputBean lender = new TagInputBean(THE_LENDER, "Lender");
        // Very specific scenario. The lender is incorrectly set to the same zipcode
        // which in turns contains bot a valid state relationship and an invalid one.
        lender.setTargets("located", new TagInputBean(ZIP, "ZipCode"));

        Tag lenderTag = tagService.createTag(su.getCompany(), lender);
        TestCase.assertEquals("Lender should be connected to one Zip", 1, tagService.findDirectedTags(lenderTag).size());
        TestCase.assertEquals("That zip should be connected to two states", 2, tagService.findDirectedTags(tagService.findDirectedTags(lenderTag).iterator().next()).size());

        Map<String, Collection<TagResultBean>> subTags = tagService.findTags(su.getCompany(), "ZipCode", ZIP, "state", "State");
        assertTrue(subTags.containsKey("state"));
        assertEquals("The zip code should be connected to two states", 2, subTags.get("state").size());

        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT-508", documentType.getName(), new DateTime(), "abc");
        entityInput.addTag(new TagInputBean(THE_LENDER, "Lender").setEntityLink("created"));
        entityInput.addTag(countyTag);  // This is the geopath to find
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);

        // The Lender is connected two a Zip code that has two different paths back to a country one via TX the other via CA
        // This is illegal data in the real world and makes accessing the Lender "located" information ambiguous.
        //
        // Using "out-of-the-box" functionality we would get an exception
        // NoSuchElementException: More than one element....

        // However, we are retrieving the geo structure via a custom query set in the DocType, ignoring
        // the invalid data relationship to find only one geodata path associated with the County

        searchService.getSearchChange(result);

        Iterable<EntityTag> iTags = entityTagService.getEntityTagsWithGeo(result.getEntity());
        int count = 0;
        for (EntityTag entityTag : iTags) {
            assertNotNull (entityTag.getGeoData());
            count ++;
        }
        assertEquals("expected a Lender and a County relationship", 2, count);
    }
}
