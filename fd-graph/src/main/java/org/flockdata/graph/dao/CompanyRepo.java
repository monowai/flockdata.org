package org.flockdata.graph.dao;

import org.flockdata.data.Company;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.model.CompanyNode;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.neo4j.driver.v1.Values.parameters;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Service
public class CompanyRepo {

    private DriverManager driverManager;

    @Autowired
    void setDriverManager(DriverManager driverManager) {
        this.driverManager = driverManager;
    }


    public Company create(Company company) {
        try (Session session = driverManager.session()) {
            return session.writeTransaction(tx -> createCompanyNode(tx, company));
        }
    }

    public Company findByCode(Company company) {
        try (Session session = driverManager.session()) {
            return session.readTransaction(tx -> findByCode(tx, company.getCode()));
        }
    }

    private static Company createCompanyNode(Transaction tx, Company company) {
        StatementResult statementResult = tx.run(
            "CREATE (company:FDCompany {name: $name, code: $code, apiKey: $apiKey}) return company",
            parameters(
                "name", company.getName(),
                "code", company.getCode(),
                "name", company.getName(),
                "apiKey", company.getApiKey())
        );

        Node eNode = statementResult.single().get("company").asNode();
        return CompanyNode.build(eNode);
    }

    private static Company findByKey(Transaction tx, String key) {
        String cmd = "match (company:FDCompany {key: $key}) return company";
        StatementResult statementResult = tx.run(cmd, parameters("apiKey", key));
        Node eNode = statementResult.single().get("company").asNode();
        return CompanyNode.build(eNode);

    }

    private static Company findByCode(Transaction tx, String code) {
        String cmd = "match (company:FDCompany {code: $code}) return company";
        StatementResult statementResult = tx.run(cmd, parameters("code", code));
        Node eNode = statementResult.single().get("company").asNode();
        return CompanyNode.build(eNode);

    }

}
