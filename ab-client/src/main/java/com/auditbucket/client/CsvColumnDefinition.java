package com.auditbucket.client;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
public class CsvColumnDefinition {
    private boolean callerRef;
    private boolean title;
    private boolean tag;
    private boolean mustExist;
    private boolean valueAsProperty;
    private boolean country;
    private String nameColumn;
    private String appendJoinText = " ";
    private String relationshipName;
    private String[] refColumns;
    private String[] metaValues;

    public String[] getRefColumns() {
        return refColumns;
    }

    public void setRefColumns(String[] refColumns) {
        this.refColumns = refColumns;
    }

    public String[] getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(String[] metaValues) {
        this.metaValues = metaValues;
    }

    public void setCallerRef(boolean callerRef) {
        this.callerRef = callerRef;
    }

    public void setTitle(boolean title) {
        this.title = title;
    }

    public void setTag(boolean tag) {
        this.tag = tag;
    }

    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
        this.tag = true;
    }

    public void setValueAsProperty(boolean valueAsProperty) {
        this.valueAsProperty = valueAsProperty;
        this.tag = true;
    }

    public void setCountry(boolean country) {
        this.country = country;
        this.tag = true;
        this.mustExist = true;
    }

    public void setNameColumn(String nameColumn) {
        this.nameColumn = nameColumn;
    }

    public void setAppendJoinText(String appendJoinText) {
        this.appendJoinText = appendJoinText;
    }

    public boolean isCallerRef() {
        return callerRef;
    }

    public boolean isTitle() {
        return title;
    }

    public boolean isTag() {
        return tag;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public boolean isValueAsProperty() {
        return valueAsProperty;
    }

    public boolean isCountry() {
        return country;
    }

    public String getNameColumn() {
        return nameColumn;
    }

    public String getAppendJoinText() {
        return appendJoinText;
    }

    public String getRelationshipName() {
        if ( relationshipName== null )
            return "undefined";
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }
}
