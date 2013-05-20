package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ISystemUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
public class SystemUser implements ISystemUser {
    @GraphId
    Long id;

    @Indexed(unique = true, indexName = "sysUserName")
    private String name = null;

    private String password;

    private String openUID;

//    @RelatedTo (elementClass = CompanyUser.class, type ="isA", direction = Direction.INCOMING)
//    private ICompanyUser companyUser;

    @Fetch
    @RelatedTo(elementClass = Company.class, type = "administers", direction = Direction.OUTGOING)
    private Company company;


    protected SystemUser() {
    }

    public SystemUser(String name, String password, ICompany company, boolean admin) {
        setName(name);
        setPassword(password);

        if (admin)
            setAdministers(company);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOpenUID() {
        return openUID;
    }

    public void setOpenUID(String openUID) {
        this.openUID = openUID;
    }

//    public ICompanyUser getCompanyUser() {
//        return companyUser;
//    }
//
//    public void setCompanyUser(ICompanyUser companyUser) {
//        this.companyUser = companyUser;
//    }

    public ICompany getCompany() {
        return company;
    }

    public void setAdministers(ICompany company) {
        this.company = (Company) company;
    }


}
