package org.flockdata.graph.dao;

import static org.neo4j.driver.v1.Values.parameters;

import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.model.CompanyNode;
import org.flockdata.graph.model.SystemUserNode;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.RegistrationBean;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Service
public class CompanyRepo {

  private DriverManager driverManager;
  private KeyGenService keyGenService;

  private static Company createCompanyNode(Transaction tx, Company company) {

    StatementResult statementResult = tx.run(
        "CREATE (company:FDCompany {name: $name, code: $code, apiKey: $apiKey}) return company",
        parameters(
            "code", company.getCode().toLowerCase(),
            "name", company.getName(),
            "apiKey", company.getApiKey())
    );

    Node eNode = statementResult.single().get("company").asNode();
    return CompanyNode.build(eNode);
  }

  private static Company findByCode(Transaction tx, String code) {
    String cmd = "match (company:FDCompany {code: $code}) return company";
    StatementResult statementResult = tx.run(cmd, parameters("code", code.toLowerCase()));
    if (!statementResult.hasNext()) {
      return null;
    }
    Node eNode = statementResult.single().get("company").asNode();
    return CompanyNode.build(eNode);

  }

  @Autowired
  void setDriverManager(DriverManager driverManager) {
    this.driverManager = driverManager;
  }

  @Autowired
  void setKeyGenService(KeyGenService keyGenService) {
    this.keyGenService = keyGenService;
  }

  public Company create(Company company) {
    try (Session session = driverManager.session()) {
      return session.writeTransaction(tx -> createCompanyNode(tx, company));
    }
  }

//    private static Company findByKey(Transaction tx, String key) {
//        String cmd = "match (company:FDCompany {key: $key}) return company";
//        StatementResult statementResult = tx.run(cmd, parameters("apiKey", key));
//        Node eNode = statementResult.single().get("company").asNode();
//        return CompanyNode.build(eNode);
//
//    }

  public Company findByCode(Company company) {
    try (Session session = driverManager.session()) {
      return session.readTransaction(tx -> findByCode(tx, company.getCode()));
    }
  }

  public SystemUser findSysUserByLogin(String login) {
    try (Session session = driverManager.session()) {
      return session.readTransaction(tx -> findSysUserByLogin(tx, login));
    }

  }

  private SystemUser findSysUserByLogin(Transaction tx, String login) {
    // @RelatedTo( type = "ACCESSES", direction = Direction.OUTGOING)
    String cmd = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) where su.login=$login return su, company";
    StatementResult statementResult = tx.run(cmd, parameters("login", login));
    if (!statementResult.hasNext()) {
      return null;
    }

    Record singleResult = statementResult.single();
    Node suNode = singleResult.get("su").asNode();
    Node cNode = singleResult.get("company").asNode();

    return SystemUserNode.build(CompanyNode.build(cNode), suNode);
  }


  public SystemUser register(Company company, RegistrationBean regBean) {
    SystemUser su = SystemUserNode.build(company, regBean, keyGenService.getUniqueKey());
    return create(su);

  }

  public SystemUser create(SystemUser systemUser) {
    assert (systemUser.getCompany() != null && systemUser.getCompany().getId() != null);
    try (Session session = driverManager.session()) {
      return session.writeTransaction(tx -> create(tx, systemUser));
    }

  }

  private SystemUser create(Transaction tx, SystemUser systemUser) {
    StatementResult statementResult = tx.run(
        "match (company) where id(company)= $companyId " +
            " CREATE (systemUser:SystemUser {name: $name, email: $email, login: $login, apiKey: $apiKey, active: $active}) " +
            " -[:ACCESSES]->(company) return systemUser",
        parameters(
            "name", systemUser.getName(),
            "login", systemUser.getLogin(),
            "email", systemUser.getEmail(),
            "apiKey", systemUser.getApiKey(),
            "active", systemUser.isActive(),
            "companyId", systemUser.getCompany().getId()

        ));

    Node eNode = statementResult.single().get("systemUser").asNode();

    return SystemUserNode.build(systemUser.getCompany(), eNode);
  }

  public SystemUser findByApiKey(String apiKey) {
    try (Session session = driverManager.session()) {
      return session.readTransaction(tx -> findByApiKey(tx, apiKey));
    }
  }

  private SystemUser findByApiKey(Transaction tx, String apiKey) {
    String cmd = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) where su.apiKey= $apiKey return su, company";
    StatementResult statementResult = tx.run(cmd, parameters("apiKey", apiKey));
    if (!statementResult.hasNext()) {
      return null;
    }

    Record singleResult = statementResult.single();
    Node suNode = singleResult.get("su").asNode();
    Node cNode = singleResult.get("company").asNode();

    return SystemUserNode.build(CompanyNode.build(cNode), suNode);

  }
}
