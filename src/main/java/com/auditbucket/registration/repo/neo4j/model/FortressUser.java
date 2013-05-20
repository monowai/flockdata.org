package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.IFortressUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import com.auditbucket.registration.model.IFortress;

@NodeEntity


public class FortressUser implements IFortressUser {
	@GraphId 
	Long id;

	@RelatedTo (elementClass = Fortress.class, type="fortressUser", direction= Direction.INCOMING)
	private IFortress fortress;

    @Indexed(indexName = "fortressUser")
    @Fetch
    private String name = null;

    protected FortressUser (){}

    public FortressUser(IFortress fortress, String fortressUserName) {
        setName(fortressUserName);
        setFortress(fortress);
    }

    public Long getId(){
        return id;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name.toLowerCase();
	}

	public IFortress getFortress() {
		return fortress;
	}

	public void setFortress(IFortress fortress) {
		this.fortress = fortress;
	}

}
