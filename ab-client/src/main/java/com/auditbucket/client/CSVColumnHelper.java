package com.auditbucket.client;

/**
 * User: mike
 * Date: 8/05/14
 * Time: 11:43 AM
 */
public class CsvColumnHelper {

    private String key;
    private String value;
    private CsvColumnDefinition columnDefinition = null;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Boolean isCallerRef() {
        return columnDefinition.isCallerRef();
    }

    public Boolean isTitle() {
        return columnDefinition.isTitle();
    }

    public Boolean isTag() {
        return columnDefinition.isTag();
    }

    public Boolean isMustExist() {
        return columnDefinition.isMustExist();
    }

    @Override
    public String toString() {
        return "CsvColumnHelper{" +
                "value='" + value + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
    public CsvColumnHelper(String column, String value, CsvColumnDefinition columnDefinition){
        this.columnDefinition = columnDefinition;
        this.key = column;
        this.value = value;
    }
    public boolean isValueAsProperty() {
        return columnDefinition.isValueAsProperty();
    }

    public boolean isCountry() {
        return columnDefinition.isCountry();
    }

    public String getNameColumn() {
        return columnDefinition.getNameColumn();
    }

    public String getAppendJoinText() {
        return columnDefinition.getAppendJoinText();
    }

    public boolean ignoreMe() {
        return columnDefinition == null;
    }

    public String getRelationshipName() {
        return columnDefinition.getRelationshipName();
    }
}
