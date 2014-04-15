package com.auditbucket.client;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 15/04/14
 * Time: 1:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagCollection implements Mappable {
    @Override
    public Importer.importer getImporter() {
        return Importer.importer.TAGS;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TAG;
    }
}
