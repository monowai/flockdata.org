package com.auditbucket.audit.model;

/**
 * Geographic type data
 * User: mike
 * Date: 28/02/14
 * Time: 4:00 PM
 */
public class GeoData {
    private String isoCode;
    private String country;

    private String state;
    private String city;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }
}
