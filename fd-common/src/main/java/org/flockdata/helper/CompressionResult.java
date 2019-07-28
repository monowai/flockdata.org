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

package org.flockdata.helper;

import java.io.UnsupportedEncodingException;

/**
 * @author mholdsworth
 * @since 20/07/2013
 */
public class CompressionResult {
  private Method method;
  private byte[] bytes;

  CompressionResult(byte[] bytes, boolean compressed) {
    this(bytes);
    if (!compressed) {
      this.method = Method.NONE;
    }
  }

  CompressionResult(String value) throws UnsupportedEncodingException {
    this();
    method = Method.NONE;
    this.bytes = value.getBytes(ObjectHelper.charSet);// DAT-75
  }

  private CompressionResult() {
  }

  CompressionResult(byte[] bytes) {
    this();
    method = Method.GZIP;
    this.bytes = bytes;
  }

  public int length() {
    return bytes.length;
  }

  public byte[] getAsBytes() {
    return bytes;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public Method getMethod() {
    return method;
  }

  public enum Method {
    NONE, GZIP
  }

}
