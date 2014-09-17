/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.track.model.KvContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * User: Mike Holdsworth
 * Since: 20/07/13
 */
public class CompressionHelper {
    public static final String PROP_COMPRESSION = "disableCompression";
    public static Charset charSet = Charset.forName("UTF-8");

    public static CompressionResult compress(String text) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // DAT-218
        boolean disableCompression = Boolean.parseBoolean(System.getProperty(PROP_COMPRESSION, "false"));
        try {
            byte[] bytes = text.getBytes(charSet);
            if (!disableCompression && bytes.length > 512) {
                OutputStream out = new GZIPOutputStream(baos);
                out.write(bytes);
                out.close();
                return new CompressionResult(baos.toByteArray());
            } else {
                // no compression
                return new CompressionResult(text);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static CompressionResult compress(KvContent content) {
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.valueToTree(content);
        String text = node.toString();
        return compress(text);
    }

    public static String decompress(CompressionResult result) {
        try {
            if (result.getBytes() == null)
                return null;
            if (result.getMethod().equals(CompressionResult.Method.NONE))
                return new String(result.getBytes(), charSet);

            InputStream in = new GZIPInputStream(new ByteArrayInputStream(result.getBytes()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0)
                baos.write(buffer, 0, len);
            return new String(baos.toByteArray(), charSet);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String decompress(byte[] what, boolean compressed) {
        CompressionResult result = new CompressionResult(what, compressed);
        return decompress(result);
    }
}
