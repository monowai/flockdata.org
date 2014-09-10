package com.auditbucket.client.csv;

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
        if (columnDefinition.getIndex() != null)
            return columnDefinition.getIndex();
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

    public Boolean isStrategy(){
        return columnDefinition.getStrategy()!=null;
    }

    public Boolean isDescription() {
        return columnDefinition.isDescription();
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

    /**
     * The name of the relationship. If relationshipName is not set then undefined
     * will be returned unless it's a country in which case the name of the column is returned
     *
     * @return non-null value the caller can use as a name for the relationship.
     */
    public String getRelationshipName() {
        String rlxName = columnDefinition.getRelationshipName();
        if (rlxName == null )
            rlxName = getKey();
        return rlxName;
    }

    public CsvColumnDefinition getColumnDefinition() {
        return columnDefinition;
    }

    public String getStrategy (){
        return columnDefinition.getStrategy();
    }


    public String[] getColumns() {
        return columnDefinition.getColumns();
    }

}
