package org.flockdata.search;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.bean.SearchChange;

/**
 * Provides centralized access to the way that FD parses data for ElasticSearch indexes
 *
 * Created by mike on 23/07/15.
 */
public class IndexHelper {

    public static final String PREFIX = "fd.";

    public static String parseIndex(Entity entity) {
        return parseIndex(entity.getFortress().getIndexName(), entity.getType());
    }

    /**
     * All types for a fortress
     * @param fortress {indexRoot}
     * @return {indexRoot}.*
     */
    public static String parseIndex(Fortress fortress) {
        return parseIndex(fortress.getIndexName(), "*");
    }

    public static String parseIndex(SearchChange searchChange) {
        return parseIndex(searchChange.getIndexName(), searchChange.getDocumentType());
    }

    public static String parseIndex(String indexRoot, String documentType) {
        //return (indexRoot +"."+documentType).toLowerCase();
        return indexRoot;
    }

    public static String parseIndex(QueryParams queryParams) {
        return getIndexRoot(queryParams.getCompany(), queryParams.getFortress());
    }

    public static String getIndexRoot(Fortress fortress) {

        return getIndexRoot(fortress.getCompany().getCode(), fortress.getCode());
    }

    // Returns the root level of the index - no Doc Type
    public static String getIndexRoot(String company, String fortress) {
        return PREFIX + company.toLowerCase() + "." + (fortress == null ? "*": fortress.toLowerCase());
    }

    public static String[] getIndexesToQuery(QueryParams queryParams) throws FlockException {
        return getIndexesToQuery(queryParams.getCompany(), queryParams.getFortress(), queryParams.getTypes());
    }

    /**
     *
     * @param company   who owns the index  (used in calculating the root)
     * @param fortress  system in the index (used in calculating the root)
     * @param types     types to scan
     *
     * @return One index line per Root+Type combination
     */
    public static String[] getIndexesToQuery(String company, String fortress, String[] types){
        int length = 1;
        if ( types !=null && types.length > 0 )
            length = types.length;
        String[] results = new String[ length];

        String indexRoot = getIndexRoot(company, fortress);
        if ( types == null || types.length ==0) {
            results[0] = indexRoot + ( indexRoot.endsWith(".*")?"":"*" );
        } else {
            int count = 0;
            for (String type : types) {
                results[count] = indexRoot ;//+ "."+type.toLowerCase();
                count ++;
            }
        }

        return results;
    }

    private static String getIndexRoot(String companyCode) {
        // ToDo: Add multi tenant test. Company must always be present if MultiTenanted
        if (companyCode == null || companyCode.equals(""))
            companyCode = "*";

        return PREFIX + companyCode.toLowerCase() + ".";
    }


    public static String parseType(Entity entity) {
        return parseType(entity.getType());
    }

    public static String parseType(String type) {
        return type.toLowerCase();
    }
}
