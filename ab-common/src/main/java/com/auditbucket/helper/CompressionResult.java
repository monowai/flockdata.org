/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.helper;

/**
 * User: Mike Holdsworth
 * Since: 20/07/13
 */
public class CompressionResult {
    private Method method;
    private byte[] bytes;
    private String value;
    private byte[] asBytes;
    private String asString;

    public int length() {
        return bytes.length;
    }

    public byte[] getAsBytes() {
        return bytes;

    }

    public boolean isCompressed() {
        return method == Method.GZIP;
    }


    public enum Method {
        NONE, GZIP;
    }

    private CompressionResult() {
    }

    public CompressionResult(byte[] bytes) {
        method = Method.GZIP;
        this.bytes = bytes;
    }

    public CompressionResult(byte[] bytes, boolean compressed) {
        this(bytes);
        if (compressed)
            this.method = Method.GZIP;
        else
            this.method = Method.NONE;
    }


    public CompressionResult(String value) {
        method = Method.NONE;
        this.bytes = value.getBytes();
    }


    public byte[] getBytes() {
        return bytes;
    }

    public Method getMethod() {
        return method;
    }

}
