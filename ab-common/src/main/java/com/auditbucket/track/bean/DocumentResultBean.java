package com.auditbucket.track.bean;

import com.auditbucket.track.model.Concept;
import com.auditbucket.track.model.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 29/08/14
 * Time: 12:28 PM
 */
public class DocumentResultBean implements Comparable<DocumentResultBean>{

    private Long id;

    public String getName() {
        return name;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getFortressCode() {
        return fortressCode;
    }

    private String name;
    private String fortressName;
    private String fortressCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ArrayList<Concept> getConcepts() {
        return concepts;
    }

    ArrayList<Concept> concepts = null;

    DocumentResultBean() {
    }

    public DocumentResultBean(DocumentType documentType) {
        this();
        this.name = documentType.getName();
//        code = documentType.getCode();
        fortressName = documentType.getFortress().getName();
        fortressCode = documentType.getFortress().getCode();
        this.id = documentType.getId();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void add(Concept concept) {

        if (concepts == null)
            concepts = new ArrayList<>();
        concepts.add(concept);
    }
    @Override
    public int compareTo(DocumentResultBean o) {
        return o.getName().compareTo(name);
    }

}
