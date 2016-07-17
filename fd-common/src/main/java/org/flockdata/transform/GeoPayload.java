/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

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
