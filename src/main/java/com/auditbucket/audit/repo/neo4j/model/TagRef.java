package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.repo.neo4j.model.Company;
import org.hibernate.validator.constraints.NotBlank;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 9:34 AM
 */
@NodeEntity
public class TagRef implements ITagRef {

    @GraphId
    private Long id;

    @Fetch
    @RelatedTo(elementClass = Company.class, type = "validTag", direction = Direction.INCOMING)
    private Company company;

    @RelatedTo(elementClass = AuditHeader.class, type = "tags")
    private Set<IAuditHeader> auditHeaders;

    @Indexed(numeric = false, indexName = "tagName")
    private String name;

    protected TagRef() {
    }

    public TagRef(@NotNull @NotBlank String tagName, @NotNull ICompany company) {
        this.name = tagName;
        this.company = (Company) company;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ICompany getCompany() {
        return company;
    }

    @Override
    public Set<IAuditHeader> getHeaders() {
        return auditHeaders;
    }
}
