package com.auditbucket.client.rest;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 8/07/14
 * Time: 10:10 AM
 */
public class StaticDataResolver implements IStaticDataResolver {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(StaticDataResolver.class);
    Map<String, TagInputBean> countriesByName = null;

    private AbRestClient restClient;

    public StaticDataResolver(AbRestClient restClient) {
        setRestClient(restClient);
    }

    public void setRestClient(AbRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * resolves the country Name to an ISO code
     *
     * @param name long name of the country
     * @return iso code to use
     */
    @Override
    public String resolveCountryISOFromName(String name) throws DatagioException {
//        if (simulateOnly)
//            return name;

        // 2 char country? it's already ISO
        if (name.length() == 2)
            return name;

        if (countriesByName == null) {

            Collection<TagInputBean> countries = restClient.getCountries();
            countriesByName = new HashMap<>(countries.size());
            for (TagInputBean next : countries) {
                countriesByName.put(next.getName().toLowerCase(), next);
            }
        }
        TagInputBean tag = countriesByName.get(name.toLowerCase());
        if (tag == null) {
            logger.error("Unable to resolve country name [{}]", name);
            return null;
        }
        return tag.getCode();
    }

    @Override
    public String resolve(String type, Map<String, String> args) {
        return null;
    }

}