package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
public class TxRef implements ITxRef {

    @GraphId
    private Long id;

    @Fetch
    @RelatedTo(elementClass = Company.class, type = "txTag", direction = Direction.INCOMING)
    private Company company;

    @RelatedTo(elementClass = AuditHeader.class, type = "txIncludes")
    private Set<IAuditHeader> auditHeaders;

    @Indexed(numeric = false, indexName = "tagName")
    private String name;

    private TxStatus txStatus = TxStatus.TX_CREATED;

    private long txDate;

    public TxStatus getTxStatus() {
        return txStatus;
    }

    public enum TxStatus {
        TX_CREATED, TX_ROLLBACK, TX_COMMITTED;
    }

    protected TxRef() {
    }

    public TxRef(@NotNull @NotBlank String tagName, @NotNull ICompany company) {
        this.name = tagName;
        this.company = (Company) company;
        setStatus(TxStatus.TX_CREATED);

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonIgnore
    public ICompany getCompany() {
        return company;
    }

    @Override
    @JsonIgnore
    public Set<IAuditHeader> getHeaders() {
        return auditHeaders;
    }

    @Override
    public Long getId() {
        return id;
    }

    public TxStatus commit() {
        return setStatus(TxStatus.TX_COMMITTED);
    }

    public TxStatus rollback() {
        return setStatus(TxStatus.TX_ROLLBACK);
    }

    private TxStatus setStatus(TxStatus txStatus) {
        TxStatus previous = this.txStatus;
        this.txStatus = txStatus;
        this.txDate = DateTime.now(DateTimeZone.UTC).getMillis();
        return previous;
    }

    @Override
    public String toString() {
        return "TxRef{" +
                "company=" + company +
                ", name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
