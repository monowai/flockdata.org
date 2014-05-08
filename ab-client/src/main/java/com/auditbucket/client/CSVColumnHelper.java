package com.auditbucket.client;

/**
 * User: mike
 * Date: 8/05/14
 * Time: 11:43 AM
 */
public class CSVColumnHelper {

    private String key;
    private String value;
    private boolean callerRef;
    private boolean title;
    private boolean tagName;
    private boolean mustExist;
    private boolean tagToValue;
    private boolean country;
    private String indirectColumn;


    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Boolean isCallerRef() {
        return callerRef;
    }

    public Boolean isTitle() {
        return title;
    }

    public Boolean isTagName() {
        return tagName;
    }

    public Boolean isMustExist() {
        return mustExist;
    }

    @Override
    public String toString() {
        return "CSVColumnHelper{" +
                "value='" + value + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    public CSVColumnHelper(String columnName, String value) {
        int i = 0;
        this.value = value;
        while (i < columnName.length()) {
            String match = Character.toString(columnName.charAt(i));
            boolean isChar = match.matches("[a-zA-z]");
            boolean isDigit = match.matches("\\d");
            if (!isChar && !isDigit) {
                switch (match) {
                    case "#":
                        title = true;
                        break;
                    case "$":
                        callerRef = true;
                        break;
                    case "@":
                        tagName = true;
                        break;
                    case "!":
                        mustExist = true;
                        break;
                    case "*":
                        tagToValue = true;
                        break;
                    case "%":
                        country = true;
                        mustExist = true;
                        tagName = true;
                        break;
                }
                i++;
            } else {
                if (i == 0)
                    key = columnName;
                else {
                    key = columnName.substring(i, columnName.length());
                }
                int indirectStart = key.indexOf('[');
                if (indirectStart > 0) {
                    int indirectEnd = key.indexOf(']');
                    indirectColumn = key.substring(indirectStart+1, indirectEnd);
                    key = key.substring(0, indirectStart - 1);

                }
                break;
            }
        }
    }

    public boolean isTagToValue() {
        return tagToValue;
    }

    public boolean isCountry() {
        return country;
    }

    public void setCountry(boolean country) {
        this.country = country;
    }

    public String getIndirectColumn() {
        return indirectColumn;
    }
}
