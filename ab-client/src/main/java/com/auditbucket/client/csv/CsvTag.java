package com.auditbucket.client.csv;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 3:51 PM
 */
public class CsvTag {
    private String column;
    private Boolean reverse =false;
    private String relationship;

    public String getColumn() {
        return column;
    }

    public void setColumn(String name) {
        this.column = name;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

}
