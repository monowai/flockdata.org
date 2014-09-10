package com.auditbucket.client.csv;

import com.auditbucket.client.common.CsvColumnTargetDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
public class CsvColumnDefinition {
    private boolean callerRef;
    private boolean title;
    private boolean tag;
    private boolean description;
    private String dateFormat;
    private boolean mustExist;
    private boolean valueAsProperty;
    private boolean country;
    private String  strategy=null;
    private String  fortress=null;
    private String  documentType=null;
    private String[] columns;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    private String index;
    private String nameColumn;
    private String appendJoinText = " ";
    private String relationshipName;
    private String[] refColumns;
    private String[] metaValues;

    @JsonDeserialize(using = CsvColumnTargetDeserializer.class)
    private ArrayList<CsvTag>targets = new ArrayList<>();

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
            return (isCountry()? null :"undefined");
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public ArrayList<CsvTag> getTargets() {
        return targets;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public String getFortress() {
        return fortress;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public boolean isDescription() {
        return description;
    }

    public void setDescription(boolean description) {
        this.description = description;
    }
}
