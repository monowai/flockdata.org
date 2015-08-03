package org.flockdata.transform;

/**
 * Created by mike on 29/07/15.
 */
public class GeoPayload {
    String sourceFormat;
    String x;
    String y;
    double xValue;
    double yValue;
    boolean storeInverted = true;

    GeoPayload (){}

    public GeoPayload (String sourceFormat, double xValue, double yValue){
        this();
        this.sourceFormat = sourceFormat;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public double getXValue() {
        return xValue;
    }

    public double getYValue() {
        return yValue;
    }

    public boolean isStoreInverted() {
        return storeInverted;
    }

    /**
     *
     * @return Column name that contains the actual value we want
     */
    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    /**
     *
     * @return Column name that contains the actual value we want
     */
    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public void setxValue(double xValue) {
        this.xValue = xValue;
    }

    public void setyValue(double yValue) {
        this.yValue = yValue;
    }
}
