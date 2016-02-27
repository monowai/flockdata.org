/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.helper;

import java.io.UnsupportedEncodingException;

/**
 * User: Mike Holdsworth
 * Since: 20/07/13
 */
public class CompressionResult {
    private Method method;
    private byte[] bytes;

    public CompressionResult(byte[] bytes, boolean compressed) {
        this(bytes);
        if (!compressed)
            this.method = Method.NONE;
    }

    public CompressionResult(String value) throws UnsupportedEncodingException {
        this();
        method = Method.NONE;
        this.bytes = value.getBytes(ObjectHelper.charSet);// DAT-75
    }

    public int length() {
        return bytes.length;
    }

    public byte[] getAsBytes() {
        return bytes;
    }

    public enum Method {
        NONE, GZIP
    }

    private CompressionResult() {
    }

    public CompressionResult(byte[] bytes) {
        this();
        method = Method.GZIP;
        this.bytes = bytes;
    }


    public byte[] getBytes() {
        return bytes;
    }

    public Method getMethod() {
        return method;
    }

}
