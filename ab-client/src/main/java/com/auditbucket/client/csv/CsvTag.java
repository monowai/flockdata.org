package com.auditbucket.client.csv;

import java.util.ArrayList;

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
    private String label;
    private ArrayList<CsvTag> targets;
    private boolean mustExist;

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
    public String getLabel() {
        if ( label == null)
            return column;
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<CsvTag> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<CsvTag>  targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "CsvTag{" +
                "column='" + column + '\'' +
                ", relationship='" + relationship + '\'' +
                ", label='" + label + '\'' +
                '}';
    }

    public boolean getMustExist() {
        return mustExist;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }
}
