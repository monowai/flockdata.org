package org.flockdata.search;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.bean.SearchChange;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides centralized access to the way that FD parses data for ElasticSearch indexes
 * <p/>
 * Created by mike on 23/07/15.
 */
public class IndexHelper {

    public static final String PREFIX = "fd.";

    public static String parseIndex(Entity entity) {
        if (entity.getSegment().isDefault())
            return entity.getSegment().getFortress().getRootIndex();
        else {
            String index = parseIndex(entity.getSegment().getFortress().getRootIndex());
            index = index + "." + entity.getSegment().getCode().toLowerCase();
            return index;
        }

    }

    /**
     * All types for a fortress
     *
     * @param fortress {indexRoot}
     * @return {indexRoot}.*
     */
    public static String parseIndex(Fortress fortress) {
        return parseIndex(fortress.getRootIndex());
    }

    public static String parseIndex(SearchChange searchChange) {
        return parseIndex(searchChange.getIndexName());
    }

    public static String parseIndex(String indexRoot) {
        //return (indexRoot +"."+documentType).toLowerCase();
        return indexRoot;
    }

    public static String parseIndex(QueryParams queryParams) {
        String indexRoot = getIndexRoot(queryParams.getCompany(), queryParams.getFortress());
        if ( isDefaultSegment(queryParams.getSegment()))
            return indexRoot;
        return String.format("%s.%s", indexRoot, queryParams.getSegment());

    }

    public static String getIndexRoot(FortressSegment segment) {

        return getIndexRoot(segment.getFortress().getCompany().getCode(), segment.getFortress().getCode());
    }

    public static String getIndexRoot(Fortress fortress) {
        return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
    }

    // Returns the root level of the index with no doctype or consideration of a segment
    public static String getIndexRoot(String company, String fortress) {
        return PREFIX + company.toLowerCase() + (fortress == null || fortress.equals("*") ? "" : "."+fortress.toLowerCase());
    }

    /**
     * Computes ES indexes, including wildcards, from the supplied query parameters
     *
     * @param queryParams
     * @return one index per doc type
     * @throws FlockException
     */
    public static String[] getIndexesToQuery(QueryParams queryParams) throws FlockException {
        return getIndexesToQuery(queryParams.getCompany(), queryParams.getFortress(), queryParams.getSegment(), queryParams.getTypes());
    }

    /**
     * @param company  owns the index
     * @param fortress owns the index data
     * @param segment  optional segment to restrict by
     * @param types    types to scan
     * @return One index line per Root+Type combination
     */
    public static String[] getIndexesToQuery(String company, String fortress, String segment, String[] types) {
        int length = 1;
        if (types != null && types.length > 0)
            length = 1;
        String[] results = new String[length];
        Collection<String> found = new ArrayList<>();

        String indexRoot = PREFIX + company.toLowerCase() ;
        String segmentFilter ="";

        if (!isDefaultSegment(segment))
            segmentFilter = "."+segment.toLowerCase();

        String fortressFilter ;
        if ( fortress == null || fortress.equals("*"))
            fortressFilter=".*";
       else
            fortressFilter = (segmentFilter.equals("")?"."+fortress.toLowerCase()+"*":"."+fortress.toLowerCase());

        indexRoot = indexRoot+fortressFilter+ segmentFilter;

        if (types == null || types.length == 0) {
            results[0] = indexRoot ;
        } else {
            int count = 0;
            for (String type : types) {
                if (!found.contains(indexRoot)) {
                    results[count] = indexRoot; //+ "."+type.toLowerCase();
                    found.add(indexRoot);
                    count++;
                }
            }
        }

        return results;
    }

    public static String parseType(Entity entity) {
        return parseType(entity.getType());
    }

    public static String parseType(String type) {
        return type.toLowerCase();
    }

    // Determines if the segment is a regular default
    private static boolean isDefaultSegment(String segment){
        return segment == null || segment.equals(FortressSegment.DEFAULT);
    }
}
