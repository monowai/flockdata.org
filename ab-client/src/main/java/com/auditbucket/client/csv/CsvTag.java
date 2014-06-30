package com.auditbucket.client.csv;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 3:51 PM
 */
public class CsvTag {
    private String column;
    private String name;
    private Boolean reverse =false;
    private String relationship;
    private String index;

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

    public String getName() {
        return name;
    }
    public void setName(String name){
        this.name= name;
    }

    /**
     *
     * @return The label node that will be created. Reverts to the column name if not defined
     */
    public String getIndex() {
        if ( index == null)
            return column;
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
