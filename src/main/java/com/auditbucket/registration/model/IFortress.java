package com.auditbucket.registration.model;


public interface IFortress {

    public abstract Long getId();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract ICompany getCompany();

    public abstract void setCompany(ICompany ownedBy);

    public String getFortressKey();

    /**
     * Are changes logged against this fortress accumulated or updated in the search engine
     *
     * @return boolean
     */
    public Boolean isAccumulatingChanges();

    public Boolean isIgnoreSearchEngine();

    public void setAccumulatingChanges(Boolean accumulateChanges);

}