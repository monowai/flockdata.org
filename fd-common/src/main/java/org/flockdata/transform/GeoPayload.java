/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.transform;

/**
 * @author mholdsworth
 * @since 29/07/2015
 */
public class GeoPayload {
  private String sourceFormat;
  private String x;
  private String y;
  private double xValue;
  private double yValue;
  private boolean storeInverted = true;

  GeoPayload() {
  }

  public GeoPayload(String sourceFormat, double xValue, double yValue) {
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
   * @return Column name that contains the actual value we want
   */
  public String getX() {
    return x;
  }

  public void setX(String x) {
    this.x = x;
  }

  /**
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
