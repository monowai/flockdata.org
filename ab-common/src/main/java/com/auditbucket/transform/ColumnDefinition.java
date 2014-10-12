package com.auditbucket.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
public class ColumnDefinition {
    private boolean  callerRef;
    private boolean  title;
    private boolean  description;
    private String   dateFormat;
    private boolean  valueAsProperty;
    private boolean  country;
    private boolean  createdDate;
    private String   strategy = null;
    private String   fortress = null;
    private String   documentType = null;
    private String   label;
    private String   type; //datatype
    private String   nameColumn;
    private String   appendJoinText = " ";
    private String   relationshipName;
    private String[] refColumns;
    private String[] metaValues;

    // ToDo: Replace this with CsvTagMapper ??
    private boolean tag;
    private boolean  mustExist;
    private String relationship;
    private Boolean reverse = false;
    private String delimiter;

    private String[] columns;
    private String code;
    private String customPropertyName;
    private boolean createdUser;
    private boolean updateUser;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonDeserialize(using = TagProfileDeserializer.class)
    private ArrayList<TagProfile> targets = new ArrayList<>();

    public String[] getRefColumns() {
        return refColumns;
    }

    public void setTag(boolean tag) {
        this.tag = tag;
    }


    public boolean isCallerRef() {
        return callerRef;
    }

    public boolean isTitle() {
        return title;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public boolean isTag() {
        return tag || isCountry();
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

    public String getRelationshipName() {
        if (relationshipName == null)
            return (isCountry() ? null : "undefined");
        return relationshipName;
    }

    public ArrayList<TagProfile> getTargets() {
        return targets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getStrategy() {
        return strategy;
    }

    public String[] getColumns() {
        return columns;
    }

    public String getFortress() {
        return fortress;
    }

    public String getDocumentType() {
        return documentType;
    }

    public boolean isDescription() {
        return description;
    }


    public String getCode() {
        return code;
    }

    public String getCustomPropertyName() {
        return customPropertyName;
    }

    public String getRelationship() {
        return relationship;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setMetaValues(String[] metaValues) {
        this.metaValues = metaValues;
    }

    public void setRefColumns(String[] refColumns) {
        this.refColumns = refColumns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(boolean createdUser) {
        this.createdUser = createdUser;
    }

    public boolean isUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(boolean updateUser) {
        this.updateUser = updateUser;
    }

    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }

    public boolean isCreatedDate() {
        return createdDate;
    }
}
