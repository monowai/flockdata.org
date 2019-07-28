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

package org.flockdata.track.bean;

import java.util.ArrayList;

/**
 * @author mholdsworth
 * @tag Contract, Matrix, Query
 * @since 12/06/2014
 */
public class MatrixInputBean {
  private ArrayList<String> documents = null;

  private ArrayList<String> fortresses = null;
  private int minCount = 1;

  private int sampleSize = 1000;

  private int maxEdges = 5000;
  private String queryString = null;

  private Boolean byKey = false;
  private ArrayList<String> concepts = null;
  private ArrayList<String> fromRlxs = null;
  private ArrayList<String> toRlxs = null;
  private Boolean reciprocalExcluded = false;
  private String sumCol;  // TBC: Should we let the user pick a column
  private boolean sumByCol = false;
  private String sumColumn = "props-value";
  private String cypher;

  public ArrayList<String> getToRlxs() {
    return toRlxs;
  }

  public void setToRlxs(ArrayList<String> toRlxs) {
    this.toRlxs = toRlxs;
  }

  public ArrayList<String> getFromRlxs() {
    return fromRlxs;
  }

  public void setFromRlxs(ArrayList<String> fromRlxs) {
    this.fromRlxs = fromRlxs;
  }

  public ArrayList<String> getConcepts() {
    return concepts;
  }

  public void setConcepts(ArrayList<String> concepts) {
    this.concepts = concepts;
  }

  public int getMinCount() {
    return minCount;
  }

  public void setMinCount(int minCount) {
    this.minCount = minCount;
  }

  public ArrayList<String> getDocuments() {
    return documents;
  }

  public void setDocuments(ArrayList<String> documents) {
    this.documents = documents;
  }

  /**
   * @return should the edges be keys or values
   */
  public boolean isByKey() {
    return byKey;
  }

  public void setByKey(boolean byKey) {
    this.byKey = byKey;
  }

  public boolean isReciprocalExcluded() {
    return reciprocalExcluded;
  }

  public void setReciprocalExcluded(boolean reciprocalExcluded) {
    this.reciprocalExcluded = reciprocalExcluded;
  }

  public String getQueryString() {
    return queryString;
  }

  public MatrixInputBean setQueryString(String queryString) {
    this.queryString = queryString;
    return this;
  }

  public ArrayList<String> getFortresses() {
    return fortresses;
  }

  public MatrixInputBean setFortresses(ArrayList<String> fortresses) {
    this.fortresses = fortresses;
    return this;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(int sampleSize) {
    this.sampleSize = sampleSize;
  }

  public int getMaxEdges() {
    return maxEdges;
  }

  @Override
  public String toString() {
    return "MatrixInputBean{" +
        "minCount=" + minCount +
        ", documents=" + documents +
        ", fortresses=" + fortresses +
        ", queryString='" + queryString + '\'' +
        ", sampleSize=" + sampleSize +
        '}';
  }

  public String getSumCol() {
    return sumCol;
  }

  public void setSumCol(String sumCol) {
    this.sumCol = sumCol;
  }

  public boolean isSumByCol() {
    return sumByCol;
  }

  public void setSumByCol(boolean sumByCol) {
    this.sumByCol = sumByCol;
  }

  public String getSumColumn() {
    return sumColumn;
  }

  public void setSumColumn(String sumColumn) {
    this.sumColumn = sumColumn;
  }

  public String getCypher() {
    return cypher;
  }

  public void setCypher(String cypher) {
    this.cypher = cypher;
  }
}
